package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.mapping.MapperRegistry
import dev.ruffrick.jda.kotlinx.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import net.dv8tion.jda.api.interactions.commands.Command as ICommand

class CommandRegistry(
    val commands: List<SlashCommand>,
    val mapperRegistry: MapperRegistry
) {

    private val log by Logger
    private val descriptions = Json.decodeFromString<Map<String, String>>(
        this::class.java.getResourceAsStream("/lang/descriptions.json")?.bufferedReader().use { it?.readText() }
            ?: throw IllegalArgumentException("Missing resource: path='/lang/descriptions.json'")
    )
    private val optionTypes = mapOf(
        String::class to OptionType.STRING,
        Long::class to OptionType.INTEGER,
        Boolean::class to OptionType.BOOLEAN,
        User::class to OptionType.USER,
        GuildChannel::class to OptionType.CHANNEL,
        Role::class to OptionType.ROLE
    )

    val commandsByName: Map<String, SlashCommand>
    val commandsByKey: Map<String, Pair<KFunction<*>, List<String>>>
    val buttonsByKey: Map<String, Pair<KFunction<*>, Boolean>>

    init {
        val commandsByName = mutableMapOf<String, SlashCommand>()
        val commandsByKey = mutableMapOf<String, Pair<KFunction<*>, List<String>>>()
        val buttonsByKey = mutableMapOf<String, Pair<KFunction<*>, Boolean>>()

        commands.forEach { slashCommand ->
            val command = slashCommand::class.findAnnotation<Command>()
                ?: throw IllegalArgumentException("${slashCommand::class.simpleName}: @Command annotation missing!")

            slashCommand.scope = command.scope
            slashCommand.requiredPermissions = command.requiredPermissions
            slashCommand.commandRegistry = this

            val commandName = command.name.ifEmpty {
                slashCommand::class.simpleName!!.removeSuffix("Command").lowercase()
            }
            val commandData = CommandData(commandName, getDescription(commandName))
            val subcommandGroups = mutableListOf<SubcommandGroupData>()

            slashCommand::class.memberFunctions.forEach { function ->
                val baseCommand = function.findAnnotation<BaseCommand>()
                val subcommand = function.findAnnotation<Subcommand>()
                val commandButton = function.findAnnotation<CommandButton>()
                when {
                    baseCommand != null -> {
                        val options = parseOptions(function, slashCommand::class, commandData.name)
                        commandData.addOptions(options)
                        commandsByKey[commandName] = function to options.map { it.name }
                    }
                    subcommand != null -> {
                        val name = subcommand.name.ifEmpty { function.name.lowercase() }
                        if (subcommand.group.isNotEmpty()) {
                            val group = subcommand.group
                            val key = "$commandName.$group.$name"
                            val options = parseOptions(function, slashCommand::class, key)
                            val subcommandData = SubcommandData(name, getDescription(key)).addOptions(options)
                            subcommandGroups.firstOrNull { subcommandGroupData ->
                                subcommandGroupData.name == group
                            }?.addSubcommands(subcommandData) ?: subcommandGroups.add(
                                SubcommandGroupData(subcommand.group, getDescription("$commandName.$group"))
                                    .addSubcommands(subcommandData)
                            )
                            commandsByKey[key] = function to options.map { it.name }
                        } else {
                            val key = "$commandName.$name"
                            val options = parseOptions(function, slashCommand::class, key)
                            val subcommandData = SubcommandData(name, getDescription(key)).addOptions(options)
                            commandData.addSubcommands(subcommandData)
                            commandsByKey[key] = function to options.map { it.name }
                        }
                    }
                    commandButton != null -> {
                        for (i in 1 until function.parameters.size) {
                            val parameter = function.parameters[i]
                            val type = parameter.type.classifier as KClass<*>
                            require(
                                type == ButtonClickEvent::class ||
                                        mapperRegistry.buttonEventMappers.containsKey(type)
                            ) { "No ButtonEventMapper found for type ${type.qualifiedName}" }
                        }
                        val id = commandButton.id.ifEmpty { function.name.lowercase() }
                        buttonsByKey["$commandName.$id"] = function to commandButton.private
                    }
                }
            }
            if (subcommandGroups.isNotEmpty()) commandData.addSubcommandGroups(subcommandGroups)

            slashCommand.commandData = commandData

            commandsByName[commandName] = slashCommand
        }

        this.commandsByName = commandsByName
        this.commandsByKey = commandsByKey
        this.buttonsByKey = buttonsByKey

        log.info("Registered ${commands.size} commands and ${buttonsByKey.size} buttons")
    }

    private fun getDescription(key: String): String {
        return descriptions[key] ?: throw IllegalArgumentException("Missing description: key='$key'")
    }

    private fun parseOptions(
        function: KFunction<*>,
        `class`: KClass<out SlashCommand>,
        root: String
    ): List<OptionData> {
        val options = mutableListOf<OptionData>()
        var allowNonOptions = true
        for (i in 1 until function.parameters.size) {
            val parameter = function.parameters[i]
            val type = parameter.type.classifier as KClass<*>
            val commandOption = parameter.findAnnotation<CommandOption>()
            if (commandOption == null) {
                require(allowNonOptions) {
                    "${`class`.qualifiedName}: Parameter ${parameter.name} in function " +
                            "${function.name} must be annotated as @CommandOption!"
                }
                require(type == SlashCommandEvent::class || mapperRegistry.commandEventMappers.containsKey(type)) {
                    "No CommandEventMapper found for type ${type.qualifiedName}"
                }
            } else {
                allowNonOptions = false
                val name = commandOption.name.ifEmpty { parameter.name!!.lowercase() }
                if (type.java.isEnum) {
                    options.add(
                        OptionData(
                            OptionType.STRING, name, getDescription("$root.$name"), !parameter.type.isMarkedNullable
                        ).addChoices(
                            type.java.enumConstants.map { ICommand.Choice(it.toString(), (it as Enum<*>).name) }
                        )
                    )
                } else {
                    val optionType = optionTypes[type]
                        ?: mapperRegistry.stringMappers[type]?.let { OptionType.STRING }
                        ?: mapperRegistry.longMappers[type]?.let { OptionType.INTEGER }
                        ?: mapperRegistry.booleanMappers[type]?.let { OptionType.BOOLEAN }
                        ?: mapperRegistry.userMappers[type]?.let { OptionType.USER }
                        ?: mapperRegistry.channelMappers[type]?.let { OptionType.CHANNEL }
                        ?: mapperRegistry.roleMappers[type]?.let { OptionType.ROLE }
                        ?: throw IllegalArgumentException("No Mapper found for type ${type.qualifiedName}")
                    options.add(
                        OptionData(optionType, name, getDescription("$root.$name"), !parameter.type.isMarkedNullable)
                    )
                }
            }
        }
        return options
    }

    fun updateCommands(shardManager: ShardManager) {
        shardManager.shardCache.forEach {
            updateCommands(it)
        }
    }

    fun updateCommands(jda: JDA) {
        jda.updateCommands()
            .addCommands(commands.map { it.commandData })
            .queue()
    }

    fun updateCommands(guild: Guild) {
        guild.updateCommands()
            .addCommands(commands.map { it.commandData })
            .queue()
    }

}
