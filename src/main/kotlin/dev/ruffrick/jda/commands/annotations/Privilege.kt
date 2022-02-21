package dev.ruffrick.jda.commands.annotations

import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Privilege(
    val type: CommandPrivilege.Type,
    val enabled: Boolean,
    val id: Long,
)
