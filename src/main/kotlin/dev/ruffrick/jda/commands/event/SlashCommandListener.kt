package dev.ruffrick.jda.commands.event

import dev.ruffrick.jda.commands.CommandRegistry
import dev.ruffrick.jda.commands.CommandScope
import dev.ruffrick.jda.commands.SlashCommand
import dev.ruffrick.jda.kotlinx.await
import dev.ruffrick.jda.kotlinx.event.SuspendEventListener
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.system.measureTimeMillis

internal class SlashCommandListener(
    private val commandRegistry: CommandRegistry
) : SuspendEventListener() {

    override suspend fun onEvent(event: GenericEvent) {
        if (event !is SlashCommandEvent) return

        val command = commandRegistry.commandsByName[event.name]
            ?: throw IllegalArgumentException("Invalid command: name='${event.name}'")

        if (event.isFromGuild) {
            val selfMember = event.guild!!.retrieveMember(event.jda.selfUser).await()!!
            if (!selfMember.hasPermission(event.guildChannel, Permission.MESSAGE_WRITE)) return event.replyEmbeds(
                EmbedBuilder().setDescription("I can't send messages in this channel!").build()
            ).setEphemeral(true).queue()
        }

        when (command.scope) {
            CommandScope.GUILD -> {
                if (!event.isFromGuild) return event.replyEmbeds(
                    EmbedBuilder().setDescription("This command is restricted to guild channels!").build()
                ).setEphemeral(true).queue()
                if (!hasRequiredPermissions(command, event)) return
            }
            CommandScope.PRIVATE -> if (event.isFromGuild) return event.replyEmbeds(
                EmbedBuilder().setDescription("This command is restricted to private channels!").build()
            ).setEphemeral(true).queue()
            CommandScope.BOTH -> if (event.isFromGuild && !hasRequiredPermissions(command, event)) return
        }

        val key = when {
            event.subcommandGroup != null -> "${event.name}.${event.subcommandGroup}.${event.subcommandName}"
            event.subcommandName != null -> "${event.name}.${event.subcommandName}"
            else -> event.name
        }

        val (function, options) = commandRegistry.commandsByKey[key]
            ?: throw IllegalArgumentException(
                "No command mapping found: " +
                        "name='${event.name}', " +
                        "subcommandGroup='${event.subcommandGroup}', " +
                        "subcommandName='${event.subcommandName}'"
            )

        val duration = measureTimeMillis {
            val parameterOffset = function.parameters.size - options.size
            val eventArgs = Array(parameterOffset - 1) {
                val type = function.parameters[it + 1].type.classifier as KClass<*>
                if (type == SlashCommandEvent::class) {
                    event
                } else {
                    commandRegistry.mapperRegistry.commandEventMappers[type]!!.transform(event)
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
                    else -> event.getOption(name)?.let { option ->
                        if (type.java.isEnum) type.java.enumConstants.first { (it as Enum<*>).name == option.asString } else try {
                            when (option.type) {
                                OptionType.STRING ->
                                    commandRegistry.mapperRegistry.stringMappers[type]!!.transform(option.asString)
                                OptionType.INTEGER ->
                                    commandRegistry.mapperRegistry.longMappers[type]!!.transform(option.asLong)
                                OptionType.BOOLEAN ->
                                    commandRegistry.mapperRegistry.booleanMappers[type]!!.transform(option.asBoolean)
                                OptionType.USER ->
                                    commandRegistry.mapperRegistry.userMappers[type]!!.transform(option.asUser)
                                OptionType.CHANNEL ->
                                    commandRegistry.mapperRegistry.channelMappers[type]!!.transform(option.asGuildChannel)
                                OptionType.ROLE ->
                                    commandRegistry.mapperRegistry.roleMappers[type]!!.transform(option.asRole)
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

        log.debug(
            "Executed command: " +
                    "key='$key', " +
                    "userId='${event.user.id}', " +
                    "guildId='${event.guild?.id ?: -1}', " +
                    "durationMs='$duration'"
        )
    }

    private fun hasRequiredPermissions(command: SlashCommand, event: SlashCommandEvent): Boolean {
        val missingPermissions = command.requiredPermissions
            .filter { event.member!!.hasPermission(event.textChannel, it) }
            .joinToString { "`${it.getName()}`" }
        if (missingPermissions.isNotEmpty()) {
            event.replyEmbeds(
                EmbedBuilder().setDescription(
                    "You don't have the required permissions to do that! " +
                            "You are missing the following permissions: $missingPermissions"
                ).build()
            ).setEphemeral(true).queue()
            return false
        }
        return true
    }

}
