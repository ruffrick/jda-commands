package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.event.ButtonClickListener
import dev.ruffrick.jda.commands.event.SlashCommandListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

fun ShardManager.registerCommands(`package`: String, classLoader: ClassLoader = this::class.java.classLoader) =
    registerCommands(loadCommands(`package`, classLoader))

fun ShardManager.registerCommands(commands: List<SlashCommand>) = CommandRegistry(commands).also {
    it.updateCommands(this)
    addEventListener(
        SlashCommandListener(it),
        ButtonClickListener(it)
    )
}

fun JDA.registerCommands(`package`: String, classLoader: ClassLoader = this::class.java.classLoader) =
    registerCommands(loadCommands(`package`, classLoader))

fun JDA.registerCommands(commands: List<SlashCommand>) = CommandRegistry(commands).also {
    it.updateCommands(this)
    addEventListener(
        SlashCommandListener(it),
        ButtonClickListener(it)
    )
}

private fun loadCommands(`package`: String, classLoader: ClassLoader): List<SlashCommand> {
    val reflections = Reflections(
        ConfigurationBuilder()
            .addClassLoader(classLoader)
            .addUrls(ClasspathHelper.forPackage(`package`, classLoader))
    )
    return reflections.getSubTypesOf(SlashCommand::class.java)
        .map { it.getConstructor().newInstance() as SlashCommand }
}
