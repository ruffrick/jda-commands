@file:Suppress("unused")

package dev.ruffrick.jda.commands

import dev.ruffrick.jda.kotlinx.Logger
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.components.Button

abstract class SlashCommand {
    lateinit var commandRegistry: CommandRegistry
    lateinit var commandData: CommandData

    protected val log by Logger

    protected fun primary(id: String, label: String) = Button.primary("${commandData.name}.$id", label)
    protected fun primary(id: String, emoji: Emoji) = Button.primary("${commandData.name}.$id", emoji)

    protected fun secondary(id: String, label: String) = Button.secondary("${commandData.name}.$id", label)
    protected fun secondary(id: String, emoji: Emoji) = Button.secondary("${commandData.name}.$id", emoji)

    protected fun success(id: String, label: String) = Button.success("${commandData.name}.$id", label)
    protected fun success(id: String, emoji: Emoji) = Button.success("${commandData.name}.$id", emoji)

    protected fun danger(id: String, label: String) = Button.danger("${commandData.name}.$id", label)
    protected fun danger(id: String, emoji: Emoji) = Button.danger("${commandData.name}.$id", emoji)

    protected fun link(url: String, label: String) = Button.link(url, label)
    protected fun link(url: String, emoji: Emoji) = Button.link(url, emoji)
}
