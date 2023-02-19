package dev.ruffrick.jda.commands.api

import dev.ruffrick.jda.commands.api.internal.CommandListener
import dev.ruffrick.jda.kotlinx.event.SuspendEventManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction

fun Guild.updateCommands(commands: List<SlashCommand>): CommandListUpdateAction {
    jda.addCommandListener(commands)
    return updateCommands().addCommands(commands.map { it.commandData })
}

fun JDA.updateCommands(commands: List<SlashCommand>): CommandListUpdateAction {
    addCommandListener(commands)
    return updateCommands().addCommands(commands.map { it.commandData })
}

fun JDA.addCommandListener(commands: List<SlashCommand>) {
    require(eventManager is SuspendEventManager) { "Cannot use CommandListener with ${eventManager::class.simpleName}" }
    if (registeredListeners.filterIsInstance<CommandListener>().none()) {
        addEventListener(CommandListener(commands))
    }
}
