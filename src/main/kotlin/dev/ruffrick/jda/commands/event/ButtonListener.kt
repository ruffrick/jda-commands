package dev.ruffrick.jda.commands.event

import dev.ruffrick.jda.commands.CommandRegistry
import dev.ruffrick.jda.kotlinx.event.SuspendEventListener
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.system.measureTimeMillis

internal class ButtonListener(
    private val commandRegistry: CommandRegistry
) : SuspendEventListener() {

    override suspend fun onEvent(event: GenericEvent) {
        if (event !is ButtonClickEvent) return

        val commandName = event.componentId.substringBefore('.')
        val command = commandRegistry.command(commandName)
            ?: throw IllegalArgumentException("Unknown button: id='${event.componentId}'")
        val function = commandRegistry.buttonFunctions[event.componentId]
            ?: throw IllegalArgumentException("Unknown button: id='${event.componentId}'")

        val duration = measureTimeMillis {
            val args = Array(function.parameters.size - 1) { i ->
                val type = function.parameters[i + 1].type.classifier as KClass<*>
                if (type == ButtonClickEvent::class) {
                    event
                } else {
                    try {
                        commandRegistry.transform(event, type)
                    } catch (e: IllegalArgumentException) {
                        return event.replyEmbeds(
                            EmbedBuilder().setDescription(e.message ?: "Something went wrong \uD83D\uDE15").build()
                        ).setEphemeral(true).queue()
                    }
                }
            }
            function.callSuspend(command, *args)
        }

        log.info(
            "Handling button '${event.componentId}' took $duration ms (userId: ${event.user.id}, guildId: ${event.guild?.id ?: -1})"
        )
    }

}
