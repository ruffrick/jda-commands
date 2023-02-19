package dev.ruffrick.jda.commands.event

import dev.ruffrick.jda.commands.CommandRegistry
import dev.ruffrick.jda.kotlinx.event.SuspendEventListener
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.system.measureTimeMillis

internal class CommandListener(
    private val commandRegistry: CommandRegistry
) : SuspendEventListener() {

    override suspend fun onEvent(event: GenericEvent) {
        if (event !is SlashCommandInteractionEvent) return

        val id = when {
            event.subcommandGroup != null -> "${event.name}.${event.subcommandGroup}.${event.subcommandName}"
            event.subcommandName != null -> "${event.name}.${event.subcommandName}"
            else -> event.name
        }
        val command = commandRegistry.command(event.name)
            ?: throw IllegalArgumentException("Unknown command: id='$id'")
        val (function, options) = commandRegistry.commandFunctions[id]
            ?: throw IllegalArgumentException("Unknown command: id='$id'")

        val duration = measureTimeMillis {
            val parameterOffset = function.parameters.size - options.size
            val eventArgs = Array(parameterOffset - 1) { i ->
                val type = function.parameters[i + 1].type.classifier as KClass<*>
                if (type == SlashCommandInteractionEvent::class) {
                    event
                } else {
                    commandRegistry.transform(event, type)
                }
            }
            val optionArgs = Array(options.size) { index ->
                val type = function.parameters[parameterOffset + index].type.classifier as KClass<*>
                val name = options[index]
                when (type) {
                    String::class -> event.getOption(name)?.asString
                    Long::class -> event.getOption(name)?.asLong
                    Boolean::class -> event.getOption(name)?.asBoolean
                    User::class -> event.getOption(name)?.asUser
                    GuildChannel::class -> event.getOption(name)?.asGuildChannel
                    Role::class -> event.getOption(name)?.asRole
                    IMentionable::class -> event.getOption(name)?.asMentionable
                    Double::class -> event.getOption(name)?.asDouble
                    else -> event.getOption(name)?.let { option ->
                        if (type.java.isEnum) type.java.enumConstants.first { (it as Enum<*>).name == option.asString } else try {
                            when (option.type) {
                                OptionType.STRING -> commandRegistry.transform(option.asString, type)
                                OptionType.INTEGER -> commandRegistry.transform(option.asLong, type)
                                OptionType.BOOLEAN -> commandRegistry.transform(option.asBoolean, type)
                                OptionType.USER -> commandRegistry.transform(option.asUser, type)
                                OptionType.CHANNEL -> commandRegistry.transform(option.asGuildChannel, type)
                                OptionType.ROLE -> commandRegistry.transform(option.asRole, type)
                                OptionType.MENTIONABLE -> commandRegistry.transform(option.asMentionable, type)
                                OptionType.NUMBER -> commandRegistry.transform(option.asDouble, type)
                                else -> throw IllegalStateException("Invalid option type: ${option.type}")
                            }
                        } catch (e: IllegalArgumentException) {
                            return event.replyEmbeds(
                                EmbedBuilder().setDescription(e.message ?: "Something went wrong \uD83D\uDE15").build()
                            ).setEphemeral(true).queue()
                        }
                    }
                }
            }
            function.callSuspend(command, *eventArgs, *optionArgs)
        }

        log.info(
            "Executing command '$id' took $duration ms (userId: ${event.user.id}, guildId: ${event.guild?.id ?: -1})"
        )
    }

}
