package dev.ruffrick.jda.commands.event

import dev.ruffrick.jda.commands.CommandRegistry
import dev.ruffrick.jda.kotlinx.event.SuspendEventListener
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.system.measureTimeMillis

internal class ButtonInteractionListener(
    private val commandRegistry: CommandRegistry
) : SuspendEventListener() {

    override suspend fun onEvent(event: GenericEvent) {
        if (event !is ButtonInteractionEvent) return

        val commandName = event.componentId.substringBefore('.')
        val command = commandRegistry.command(commandName) ?: return
        val function = commandRegistry.buttonFunctions[event.componentId]
            ?: throw IllegalArgumentException("Unknown button ${event.componentId}")

        val duration = measureTimeMillis {
            val args = Array(function.parameters.size - 1) { i ->
                val type = function.parameters[i + 1].type.classifier as KClass<*>
                if (type == ButtonInteractionEvent::class) {
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

        log.info("Button ${event.componentId} handled durationMs=$duration userId=${event.user.id} guildId=${event.guild?.id ?: -1}")
    }

}
