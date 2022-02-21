package dev.ruffrick.jda.commands.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class Privileges(
    val guildId: Long,
    vararg val privileges: Privilege
)