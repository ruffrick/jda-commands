package dev.ruffrick.jda.commands

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class SlashCommand {

    lateinit var scope: CommandScope
    lateinit var requiredPermissions: Array<Permission>
    lateinit var commandRegistry: CommandRegistry
    lateinit var commandData: CommandData

    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

}
