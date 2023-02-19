package dev.ruffrick.jda.commands.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Subcommand(
    val name: String = "",
    val description: String = "",
    val group: String = "",
    val groupDescription: String = "",
)
