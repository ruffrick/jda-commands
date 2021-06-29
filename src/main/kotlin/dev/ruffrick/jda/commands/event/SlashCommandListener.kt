package dev.ruffrick.jda.commands.event

import dev.ruffrick.jda.commands.CommandRegistry
import dev.ruffrick.jda.commands.CommandScope
import dev.ruffrick.jda.commands.SlashCommand
import dev.ruffrick.jda.kotlinx.await
import dev.ruffrick.jda.kotlinx.event.SuspendEventListener
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
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

        val args = Array(options.size) {
            val (type, name) = options[it]
            when (type) {
                OptionType.STRING -> event.getOption(name)?.asString
                OptionType.INTEGER -> event.getOption(name)?.asLong
                OptionType.BOOLEAN -> event.getOption(name)?.asBoolean
                OptionType.USER -> event.getOption(name)?.asUser
                OptionType.CHANNEL -> event.getOption(name)?.asGuildChannel
                OptionType.ROLE -> event.getOption(name)?.asRole
                else -> throw IllegalArgumentException("Invalid option: name='$name', type='$type'")
            }
        }

        val duration = measureTimeMillis {
            function.callSuspend(command, event, *args)
        }
        log.debug(
            "Command executed: " +
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
                EmbedBuilder()
                    .setDescription(
                        "You don't have the required permissions to do that! " +
                                "You are missing the following permissions: $missingPermissions"
                    )
                    .build()
            ).setEphemeral(true).queue()
            return false
        }
        return true
    }

}
