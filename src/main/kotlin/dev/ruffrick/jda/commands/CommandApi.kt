package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.event.CommandListener
import dev.ruffrick.jda.commands.event.button.ButtonClickListener
import dev.ruffrick.jda.commands.event.command.SlashCommandListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

object CommandApi {

    fun registerCommands(
        shardManager: ShardManager,
        `package`: String,
        classLoader: ClassLoader = this::class.java.classLoader
    ): CommandRegistry {
        return registerCommands(shardManager, loadCommands(`package`, classLoader))
    }

    fun registerCommands(shardManager: ShardManager, commands: List<SlashCommand>): CommandRegistry {
        return CommandRegistry(commands).also {
            it.updateCommands(shardManager)
            shardManager.addEventListener(
                CommandListener(
                    SlashCommandListener(it),
                    ButtonClickListener(it)
                )
            )
        }
    }

    fun registerCommands(
        jda: JDA,
        `package`: String,
        classLoader: ClassLoader = this::class.java.classLoader
    ): CommandRegistry {
        return registerCommands(jda, loadCommands(`package`, classLoader))
    }

    fun registerCommands(jda: JDA, commands: List<SlashCommand>): CommandRegistry {
        return CommandRegistry(commands).also {
            it.updateCommands(jda)
            jda.addEventListener(
                CommandListener(
                    SlashCommandListener(it),
                    ButtonClickListener(it)
                )
            )
        }
    }

    private fun loadCommands(`package`: String, classLoader: ClassLoader): List<SlashCommand> {
        val reflections = Reflections(
            ConfigurationBuilder()
                .addClassLoader(classLoader)
                .addUrls(ClasspathHelper.forPackage(`package`, classLoader))
        )
        return reflections.getTypesAnnotatedWith(Command::class.java)
            .map { it.getConstructor().newInstance() as SlashCommand }
    }

}

fun ShardManager.registerCommands(`package`: String, classLoader: ClassLoader = this::class.java.classLoader) =
    CommandApi.registerCommands(this, `package`, classLoader)

fun ShardManager.registerCommands(commands: List<SlashCommand>) =
    CommandApi.registerCommands(this, commands)

fun JDA.registerCommands(`package`: String, classLoader: ClassLoader = this::class.java.classLoader) =
    CommandApi.registerCommands(this, `package`, classLoader)

fun JDA.registerCommands(commands: List<SlashCommand>) =
    CommandApi.registerCommands(this, commands)
