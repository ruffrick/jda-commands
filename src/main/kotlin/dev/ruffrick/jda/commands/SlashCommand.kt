package dev.ruffrick.jda.commands

import dev.ruffrick.jda.kotlinx.Logger
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.build.CommandData

abstract class SlashCommand {

    lateinit var scope: CommandScope
    lateinit var requiredPermissions: Array<Permission>
    lateinit var commandRegistry: CommandRegistry
    lateinit var commandData: CommandData

    protected val log by Logger

}
