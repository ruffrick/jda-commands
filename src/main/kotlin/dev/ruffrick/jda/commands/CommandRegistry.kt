@file:Suppress("unused")

package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.annotations.*
import dev.ruffrick.jda.commands.event.ButtonInteractionListener
import dev.ruffrick.jda.commands.event.SlashCommandInteractionListener
import dev.ruffrick.jda.commands.mapping.Mapper
import dev.ruffrick.jda.kotlinx.LogFactory
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

class CommandRegistry(
    val commands: List<SlashCommand>,
    val mappers: List<Mapper<Any, Any?>>,
) {
    private var firstUpdate = true

    private val log by LogFactory
    private val optionTypes = mapOf(
        String::class to OptionType.STRING,
        Long::class to OptionType.INTEGER,
        Boolean::class to OptionType.BOOLEAN,
        User::class to OptionType.USER,
        Channel::class to OptionType.CHANNEL,
        Role::class to OptionType.ROLE,
        IMentionable::class to OptionType.MENTIONABLE,
        Double::class to OptionType.NUMBER,
    )

    internal val commandFunctions = mutableMapOf<String, Pair<KFunction<*>, List<String>>>()
    internal val buttonFunctions = mutableMapOf<String, KFunction<*>>()

    init {
        for (command in commands) {
            val commandAnnotation = command::class.findAnnotation<Command>() ?: continue

            val commandName = commandAnnotation.name.ifEmpty {
                command::class.simpleName!!.removeSuffix("Command").lowercase()
            }
            val commandData = Commands.slash(commandName, commandAnnotation.description.ifEmpty { commandName })
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
                        val subcommandData = SubcommandData(subcommandName,
                            subcommand.description.ifEmpty { subcommandName }).addOptions(options)
                        subcommandGroups.firstOrNull { subcommandGroupData ->
                            subcommandGroupData.name == subcommandGroupName
                        }?.addSubcommands(subcommandData) ?: subcommandGroups.add(
                            SubcommandGroupData(
                                subcommand.group,
                                subcommand.groupDescription.ifEmpty { subcommandName }).addSubcommands(subcommandData)
                        )
                        commandFunctions["$commandName.$subcommandGroupName.$subcommandName"] =
                            function to options.map { it.name }
                    } else {
                        commandData.addSubcommands(
                            SubcommandData(
                                subcommandName,
                                subcommand.description.ifEmpty { subcommandName }).addOptions(options)
                        )
                        commandFunctions["$commandName.$subcommandName"] = function to options.map { it.name }
                    }
                } ?: function.findAnnotation<Button>()?.let { button ->
                    for (i in 1 until function.parameters.size) {
                        val parameter = function.parameters[i]
                        val type = parameter.type.classifier as KClass<*>
                        require(type == ButtonInteractionEvent::class || mappers.any { it.input == ButtonInteractionEvent::class && it.output == type }) {
                            "Mapper<ButtonInteractionEvent, ${type.simpleName}> not found"
                        }
                    }
                    buttonFunctions["$commandName.${button.id.ifEmpty { function.name.lowercase() }}"] = function
                }
            }
            if (subcommandGroups.isNotEmpty()) {
                commandData.addSubcommandGroups(subcommandGroups)
            }

            if (command::class.hasAnnotation<Permissions>()) {
                commandData.defaultPermissions = DefaultMemberPermissions.enabledFor(*command::class.findAnnotation<Permissions>()!!.permissions)
            }

            command.commandRegistry = this
            command.commandData = commandData

            log.info("Command $commandName registered subcommandGroups=${
                commandData.subcommandGroups.joinToString { it.name }.ifEmpty { null }
            } subcommands=${commandData.subcommands.joinToString { it.name }.ifEmpty { null }}"
            )
        }
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
                    "Parameter ${parameter.name} in function " + "${function.name} must be annotated as @Option!"
                }
                require(type == SlashCommandInteractionEvent::class || mappers.any { it.input == SlashCommandInteractionEvent::class && it.output == type }) {
                    "Mapper<SlashCommandInteractionEvent, ${type.simpleName}> not found"
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
            shardManager.addEventListener(
                SlashCommandInteractionListener(this), ButtonInteractionListener(this)
            )
        }
        shardManager.shardCache.forEach { updateCommands(it) }
    }

    fun updateCommands(jda: JDA) {
        if (firstUpdate) {
            firstUpdate = false
            jda.addEventListener(
                SlashCommandInteractionListener(this), ButtonInteractionListener(this)
            )
        }
        jda.updateCommands().addCommands(commands.map { it.commandData }).queue()
    }

    fun updateCommands(guild: Guild) {
        if (firstUpdate) {
            firstUpdate = false
            guild.jda.addEventListener(
                SlashCommandInteractionListener(this), ButtonInteractionListener(this)
            )
        }
        guild.updateCommands().addCommands(commands.map { it.commandData }).queue()
    }
}
