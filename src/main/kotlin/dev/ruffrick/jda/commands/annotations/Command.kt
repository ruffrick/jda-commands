package dev.ruffrick.jda.commands.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String = "",
    val description: String = "",
)