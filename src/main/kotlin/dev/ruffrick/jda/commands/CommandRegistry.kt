@file:Suppress("unused")

package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.event.ButtonListener
import dev.ruffrick.jda.commands.event.CommandListener
import dev.ruffrick.jda.commands.mapping.Mapper
import dev.ruffrick.jda.kotlinx.Logger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
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

class CommandRegistry(
    val commands: List<SlashCommand>,
    val mappers: List<Mapper<Any, Any?>>,
) {
    private var firstUpdate = true

    private val log by Logger
    private val optionTypes = mapOf(
        String::class to OptionType.STRING,
        Long::class to OptionType.INTEGER,
        Boolean::class to OptionType.BOOLEAN,
        User::class to OptionType.USER,
        GuildChannel::class to OptionType.CHANNEL,
        Role::class to OptionType.ROLE,
        IMentionable::class to OptionType.MENTIONABLE,
        Double::class to OptionType.NUMBER,
    )

    internal val commandFunctions: Map<String, Pair<KFunction<*>, List<String>>>
    internal val buttonFunctions: Map<String, KFunction<*>>

    init {
        val commandFunctions = mutableMapOf<String, Pair<KFunction<*>, List<String>>>()
        val buttonFunctions = mutableMapOf<String, KFunction<*>>()

        for (command in commands) {
            val commandAnnotation = command::class.findAnnotation<Command>()
                ?: throw IllegalArgumentException("${command::class.simpleName}: @Command annotation missing!")


            val commandName = commandAnnotation.name.ifEmpty {
                command::class.simpleName!!.removeSuffix("Command").lowercase()
            }
            val commandData = CommandData(commandName, commandAnnotation.description.ifEmpty { commandName })
            val subcommandGroups = mutableListOf<SubcommandGroupData>()

            for (function in command::class.memberFunctions) {
                function.findAnnotation<Command>()?.let {
                    val options = parseOptions(function)
                    commandData.addOptions(options)
                    commandFunctions[commandName] = function to options.map { it.name }
                } ?: function.findAnnotation<Subcommand>()?.let { subcommand ->
                    val options = parseOptions(function)
                    val subcommandName = subcommand.name.ifEmpty { function.name.lowercase() }
                    if (subcommand.group.isNotEmpty()) {
                        val subcommandGroupName = subcommand.group
                        val subcommandData = SubcommandData(
                            subcommandName,
                            subcommand.description.ifEmpty { subcommandName }).addOptions(options)
                        subcommandGroups.firstOrNull { subcommandGroupData ->
                            subcommandGroupData.name == subcommandGroupName
                        }?.addSubcommands(subcommandData) ?: subcommandGroups.add(
                            SubcommandGroupData(subcommand.group,
                                subcommand.groupDescription.ifEmpty { subcommandName }).addSubcommands(subcommandData)
                        )
                        commandFunctions["$commandName.$subcommandGroupName.$subcommandName"] =
                            function to options.map { it.name }
                    } else {
                        commandData.addSubcommands(
                            SubcommandData(subcommandName,
                                subcommand.description.ifEmpty { subcommandName }).addOptions(options)
                        )
                        commandFunctions["$commandName.$subcommandName"] = function to options.map { it.name }
                    }
                } ?: function.findAnnotation<Button>()?.let { button ->
                    for (i in 1 until function.parameters.size) {
                        val parameter = function.parameters[i]
                        val type = parameter.type.classifier as KClass<*>
                        require(type == ButtonClickEvent::class || mappers.any { it.input == ButtonClickEvent::class && it.output == type }) {
                            "No ButtonEventMapper found for type ${type.qualifiedName}"
                        }
                    }
                    buttonFunctions["$commandName.${button.id.ifEmpty { function.name.lowercase() }}"] = function
                }
            }
            if (subcommandGroups.isNotEmpty()) {
                commandData.addSubcommandGroups(subcommandGroups)
            }
            command.commandRegistry = this
            command.commandData = commandData
        }

        this.commandFunctions = commandFunctions
        this.buttonFunctions = buttonFunctions

        log.info("Registered ${commandFunctions.size} commands and ${buttonFunctions.size} buttons")
    }

    private fun parseOptions(function: KFunction<*>): List<OptionData> {
        val options = mutableListOf<OptionData>()
        var allowNonOptions = true
        for (i in 1 until function.parameters.size) {
            val parameter = function.parameters[i]
            val type = parameter.type.classifier as KClass<*>
            val option = parameter.findAnnotation<Option>()
            if (option == null) {
                require(allowNonOptions) {
                    "Parameter ${parameter.name} in function " + "${function.name} must be annotated as @CommandOption!"
                }
                require(type == SlashCommandEvent::class || mappers.any { it.input == SlashCommandEvent::class && it.output == type }) {
                    "Mapper<SlashCommandEvent, ${type.simpleName}> not found"
                }
            } else {
                allowNonOptions = false
                val name = option.name.ifEmpty { parameter.name!!.lowercase() }
                if (type.java.isEnum) {
                    options.add(
                        OptionData(
                            OptionType.STRING,
                            name,
                            option.description.ifEmpty { name },
                            !parameter.type.isMarkedNullable
                        ).addChoices(type.java.enumConstants.map { Choice(it.toString(), (it as Enum<*>).name) })
                    )
                } else {
                    val optionType = optionTypes[type] ?: mappers.firstOrNull { it.output == type }?.type
                    ?: throw IllegalArgumentException("Mapper<?, ${type.simpleName}> not found")
                    options.add(
                        OptionData(
                            optionType, name, option.description.ifEmpty { name }, !parameter.type.isMarkedNullable
                        )
                    )
                }
            }
        }
        return options
    }

    fun command(name: String) = commands.firstOrNull { it.commandData.name == name }

    suspend inline fun <reified S : Any> transform(value: S, type: KClass<*>) =
        mappers.first { it.input == S::class && it.output == type }.transform(value)

    fun updateCommands(shardManager: ShardManager) {
        if (firstUpdate) {
            firstUpdate = false
            shardManager.addEventListener(CommandListener(this), ButtonListener(this))
        }
        shardManager.shardCache.forEach { updateCommands(it) }
    }

    fun updateCommands(jda: JDA) {
        if (firstUpdate) {
            firstUpdate = false
            jda.addEventListener(CommandListener(this), ButtonListener(this))
        }
        jda.updateCommands().addCommands(commands.map { it.commandData }).queue()
    }

    fun updateCommands(guild: Guild) {
        if (firstUpdate) {
            firstUpdate = false
            guild.jda.addEventListener(CommandListener(this), ButtonListener(this))
        }
        guild.updateCommands().addCommands(commands.map { it.commandData }).queue()
    }
}
