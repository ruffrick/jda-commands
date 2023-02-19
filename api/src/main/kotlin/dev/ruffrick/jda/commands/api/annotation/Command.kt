package dev.ruffrick.jda.commands.api.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Command(
    val name: String = "",
    val description: String = "",
)
