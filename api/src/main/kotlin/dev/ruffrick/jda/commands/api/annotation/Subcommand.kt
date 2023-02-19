package dev.ruffrick.jda.commands.api.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Subcommand(
    val name: String = "",
    val description: String = "",
)
