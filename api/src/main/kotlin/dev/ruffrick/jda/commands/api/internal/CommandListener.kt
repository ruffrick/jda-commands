package dev.ruffrick.jda.commands.api.internal

import dev.ruffrick.jda.commands.api.SlashCommand
import dev.ruffrick.jda.kotlinx.event.SuspendEventListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

internal class CommandListener(private val commands: List<SlashCommand>) : SuspendEventListener() {
    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> commands.forEach { it.onSlashCommandInteraction(event) }
            is ButtonInteractionEvent -> commands.forEach { it.onButtonInteraction(event) }
        }
    }
}
