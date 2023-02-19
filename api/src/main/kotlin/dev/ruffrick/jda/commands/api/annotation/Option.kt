package dev.ruffrick.jda.commands.api.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Option(
    val name: String = "",
    val description: String = "",
)
