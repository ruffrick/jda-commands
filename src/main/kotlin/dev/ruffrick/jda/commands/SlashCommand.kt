@file:Suppress("unused")

package dev.ruffrick.jda.commands

import dev.ruffrick.jda.kotlinx.LogFactory
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege
import net.dv8tion.jda.api.interactions.components.buttons.Button

abstract class SlashCommand {
    internal lateinit var commandRegistry: CommandRegistry
    internal lateinit var commandData: CommandData
    internal lateinit var commandPrivileges: Map<Long, List<CommandPrivilege>>

    protected val log by LogFactory

    protected fun Btn(id: String, label: String) = Btn("${commandData.name}.$id", label, null)
    protected fun Btn(id: String, emoji: Emoji) = Btn("${commandData.name}.$id", null, emoji)

    protected data class Btn(val id: String, val label: String?, val emoji: Emoji?) {
        fun primary() = label?.let { Button.primary(id, it) } ?: emoji?.let { Button.primary(id, it) }
        fun secondary() = label?.let { Button.secondary(id, it) } ?: emoji?.let { Button.secondary(id, it) }
        fun success() = label?.let { Button.success(id, it) } ?: emoji?.let { Button.success(id, it) }
        fun danger() = label?.let { Button.danger(id, it) } ?: emoji?.let { Button.danger(id, it) }
    }
}
