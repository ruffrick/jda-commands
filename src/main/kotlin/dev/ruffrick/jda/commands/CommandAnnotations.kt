package dev.ruffrick.jda.commands

import net.dv8tion.jda.api.Permission

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Command(
    val name: String = "",
    val scope: CommandScope = CommandScope.BOTH,
    val requiredPermissions: Array<Permission> = []
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BaseCommand

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Subcommand(
    val name: String = "",
    val group: String = ""
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CommandOption(
    val name: String = ""
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CommandButton(
    val id: String = "",
    val private: Boolean = false
)
