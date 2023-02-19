package dev.ruffrick.jda.commands

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String = "",
    val description: String = "",
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Subcommand(
    val name: String = "",
    val description: String = "",
    val group: String = "",
    val groupDescription: String = "",
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Option(
    val name: String = "",
    val description: String = "",
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Button(
    val id: String = "",
)
