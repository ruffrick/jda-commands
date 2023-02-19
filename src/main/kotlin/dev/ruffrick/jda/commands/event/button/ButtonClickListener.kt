package dev.ruffrick.jda.commands.event.button

import dev.ruffrick.jda.commands.CommandRegistry
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import org.slf4j.LoggerFactory
import kotlin.reflect.full.callSuspend
import kotlin.system.measureTimeMillis

internal class ButtonClickListener(
    private val commandRegistry: CommandRegistry
) {

    private val start = System.currentTimeMillis()
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun onEvent(event: ButtonClickEvent) {
        if ((event.messageIdLong shr 22) + 1420070400000 < start) return

        val (commandName, buttonId, userId) = event.componentId.split('.').takeIf { it.size == 3 }
            ?: throw IllegalArgumentException("Invalid button: id='${event.componentId}'")

        val command = commandRegistry.commandsByName[commandName]
            ?: throw IllegalArgumentException("Invalid button: id='${event.componentId}'")

        val userIdLong = userId.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid button: id='${event.componentId}'")

        val (function, private) = commandRegistry.buttonsByKey["$commandName.$buttonId"]
            ?: throw IllegalArgumentException("No button mapping found: id='${event.componentId}'")

        if (private && event.user.idLong != userIdLong) {
            return event.replyEmbeds(
                EmbedBuilder().setDescription("This can only be used by <@$userId>!").build()
            ).setEphemeral(true).queue()
        }

        val duration = measureTimeMillis {
            function.callSuspend(command, event, userIdLong)
        }
        log.debug(
            "Button executed: " +
                    "id='${event.componentId}', " +
                    "userId='${event.user.id}', " +
                    "guildId='${event.guild?.id ?: -1}', " +
                    "durationMs='$duration'"
        )
    }

}
