package dev.ruffrick.jda.commands.api

import dev.ruffrick.jda.kotlinx.LogFactory
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.components.buttons.Button

abstract class SlashCommand {
    protected val log by LogFactory

    abstract val commandData: CommandData

    abstract suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent)
    abstract suspend fun onButtonInteraction(event: ButtonInteractionEvent)

    protected fun Btn(id: String, label: String) = Btn("${commandData.name}.$id", label, null)
    protected fun Btn(id: String, emoji: Emoji) = Btn("${commandData.name}.$id", null, emoji)

    protected data class Btn(val id: String, val label: String?, val emoji: Emoji?) {
        fun primary() = label?.let { Button.primary(id, it) } ?: emoji?.let { Button.primary(id, it) }
        fun secondary() = label?.let { Button.secondary(id, it) } ?: emoji?.let { Button.secondary(id, it) }
        fun success() = label?.let { Button.success(id, it) } ?: emoji?.let { Button.success(id, it) }
        fun danger() = label?.let { Button.danger(id, it) } ?: emoji?.let { Button.danger(id, it) }
    }
}
