package dev.ruffrick.jda.commands.event

import dev.ruffrick.jda.commands.event.button.ButtonClickListener
import dev.ruffrick.jda.commands.event.command.SlashCommandListener
import dev.ruffrick.jda.commands.util.task.TaskManager
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory

internal class CommandListener(
    private val slashCommandListener: SlashCommandListener,
    private val buttonClickListener: ButtonClickListener
) : EventListener {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun onEvent(event: GenericEvent) {
        TaskManager.async {
            try {
                when (event) {
                    is SlashCommandEvent -> slashCommandListener.onEvent(event)
                    is ButtonClickEvent -> buttonClickListener.onEvent(event)
                }
            } catch (e: Exception) {
                log.error(e.message)
                e.printStackTrace()
            }
        }
    }

}
