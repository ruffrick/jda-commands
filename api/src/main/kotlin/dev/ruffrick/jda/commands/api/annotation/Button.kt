package dev.ruffrick.jda.commands.api.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Button(
    val id: String = "",
)
