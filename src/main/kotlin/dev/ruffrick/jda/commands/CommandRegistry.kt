package dev.ruffrick.jda.commands

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
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

class CommandRegistry(
    private val commands: List<SlashCommand>
) {

    private val log = LoggerFactory.getLogger(this::class.java)
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

    val commandsByName = mutableMapOf<String, SlashCommand>()
    val commandsByKey = mutableMapOf<String, Pair<KFunction<*>, List<Pair<OptionType, String>>>>()
    val buttonsByKey = mutableMapOf<String, Pair<KFunction<*>, Boolean>>()

    init {
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
                when {
                    function.hasAnnotation<BaseCommand>() -> {
                        val options = parseOptions(function, slashCommand, commandData.name)
                        commandData.addOptions(options)
                        commandsByKey[commandName] = function to options.map { it.type to it.name }
                    }
                    function.hasAnnotation<Subcommand>() -> {
                        val subcommand = function.findAnnotation<Subcommand>()!!
                        val name = subcommand.name.ifEmpty { function.name.lowercase() }
                        if (subcommand.group.isNotEmpty()) {
                            val group = subcommand.group
                            val root = "$commandName.$group.$name"
                            val options = parseOptions(function, slashCommand, root)
                            val subcommandData = SubcommandData(name, getDescription(root))
                                .addOptions(options)
                            subcommandGroups.firstOrNull { subcommandGroupData ->
                                subcommandGroupData.name == subcommand.group
                            }?.addSubcommands(subcommandData) ?: subcommandGroups.add(
                                SubcommandGroupData(subcommand.group, getDescription("$commandName.$group"))
                                    .addSubcommands(subcommandData)
                            )
                            commandsByKey[root] = function to options.map { it.type to it.name }
                        } else {
                            val root = "$commandName.$name"
                            val options = parseOptions(function, slashCommand, root)
                            val subcommandData = SubcommandData(name, getDescription(root)).addOptions(options)
                            commandData.addSubcommands(subcommandData)
                            commandsByKey[root] = function to options.map { it.type to it.name }
                        }
                    }
                    function.hasAnnotation<CommandButton>() -> {
                        require(function.parameters.size == 3) {
                            "Function ${function.name} in class ${slashCommand::class.simpleName} must have 2 " +
                                    "arguments of type (ButtonClickEvent, Long)!"
                        }
                        val parameter1 = function.parameters[1]
                        val classifier1 = parameter1.type.classifier
                        require(classifier1 == ButtonClickEvent::class) {
                            "Parameter ${parameter1.name} in function ${function.name} in class " +
                                    "${slashCommand::class.simpleName} must be of type ButtonClickEvent but is of " +
                                    "type ${(classifier1 as KClass<*>).simpleName}!"
                        }
                        val parameter2 = function.parameters[2]
                        val classifier2 = parameter2.type.classifier
                        require(classifier2 == Long::class) {
                            "Parameter ${parameter2.name} in function ${function.name} in class " +
                                    "${slashCommand::class.simpleName} must be of type Long but is of type " +
                                    "${(classifier2 as KClass<*>).simpleName}!"
                        }
                        val commandButton = function.findAnnotation<CommandButton>()!!
                        val id = commandButton.id.ifEmpty { function.name.lowercase() }
                        buttonsByKey["$commandName.$id"] = function to commandButton.private
                    }
                }
            }
            if (subcommandGroups.isNotEmpty()) commandData.addSubcommandGroups(subcommandGroups)

            slashCommand.commandData = commandData

            commandsByName[commandName] = slashCommand
        }

        log.info("Registered ${commands.size} commands and ${buttonsByKey.size} buttons")
    }

    private fun getDescription(key: String): String {
        return descriptions[key] ?: throw IllegalArgumentException("Missing description: key='$key'")
    }

    private fun parseOptions(function: KFunction<*>, command: SlashCommand, root: String): List<OptionData> {
        val options = mutableListOf<OptionData>()
        function.parameters.forEach { parameter ->
            val classifier = parameter.type.classifier
            when {
                parameter.index == 0 -> require(classifier == command::class) { "How did we get here?" }
                parameter.index == 1 -> require(classifier == SlashCommandEvent::class) {
                    "Parameter ${parameter.name} in function ${function.name} in class ${command::class.simpleName} " +
                            "must be of type SlashCommandEvent but is of type ${(classifier as KClass<*>).simpleName}!"
                }
                parameter.index > 1 -> {
                    val commandOption = requireNotNull(parameter.findAnnotation<CommandOption>()) {
                        "Parameter ${parameter.name} in function ${function.name} in class " +
                                "${command::class.simpleName} must be annotated with @CommandOption!"
                    }
                    val type = requireNotNull(optionTypes[classifier]) {
                        "Parameter ${parameter.name} in function ${function.name} in class " +
                                "${command::class.simpleName} must be of type ${
                                    optionTypes.keys.joinToString { it.simpleName!! }
                                } but is of type ${(classifier as KClass<*>).simpleName}!"
                    }
                    val name = commandOption.name.ifEmpty { parameter.name!!.lowercase() }
                    options.add(
                        OptionData(type, name, getDescription("$root.$name"), !parameter.type.isMarkedNullable)
                    )
                }
            }
        }
        return options
    }

    fun updateCommands(shardManager: ShardManager) {
        shardManager.shards.forEach {
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