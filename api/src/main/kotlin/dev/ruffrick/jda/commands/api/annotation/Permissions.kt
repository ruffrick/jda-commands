package dev.ruffrick.jda.commands.api.annotation

import net.dv8tion.jda.api.Permission

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Permissions(
    vararg val value: Permission,
)
