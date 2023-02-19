package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.event.ButtonClickListener
import dev.ruffrick.jda.commands.event.SlashCommandListener
import dev.ruffrick.jda.commands.mapping.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.reflections.Reflections

fun ShardManager.registerCommands(
    `package`: String,
    classLoader: ClassLoader = this::class.java.classLoader
): CommandRegistry {
    val (commands, mapperRegistry) = registerPackage(`package`, classLoader)
    return registerCommands(commands, mapperRegistry)
}

fun ShardManager.registerCommands(
    commands: List<SlashCommand>,
    mapperRegistry: MapperRegistry = MapperRegistry()
) = CommandRegistry(commands, mapperRegistry).also {
    it.updateCommands(this)
    addEventListener(
        SlashCommandListener(it),
        ButtonClickListener(it)
    )
}

fun JDA.registerCommands(
    `package`: String,
    classLoader: ClassLoader = this::class.java.classLoader
): CommandRegistry {
    val (commands, mapperRegistry) = registerPackage(`package`, classLoader)
    return registerCommands(commands, mapperRegistry)
}

fun JDA.registerCommands(
    commands: List<SlashCommand>,
    mapperRegistry: MapperRegistry = MapperRegistry()
) = CommandRegistry(commands, mapperRegistry).also {
    it.updateCommands(this)
    addEventListener(
        SlashCommandListener(it),
        ButtonClickListener(it)
    )
}

private fun registerPackage(
    `package`: String,
    classLoader: ClassLoader
): Pair<List<SlashCommand>, MapperRegistry> {
    val reflections = Reflections(`package`, classLoader)
    val commands = reflections.getSubTypesOf(SlashCommand::class.java)
        .map { it.getConstructor().newInstance() as SlashCommand }
    val stringMappers = reflections.getSubTypesOf(StringMapper::class.java)
        .map { it.getConstructor().newInstance() as StringMapper }
    val longMappers = reflections.getSubTypesOf(LongMapper::class.java)
        .map { it.getConstructor().newInstance() as LongMapper }
    val booleanMappers = reflections.getSubTypesOf(BooleanMapper::class.java)
        .map { it.getConstructor().newInstance() as BooleanMapper }
    val userMappers = reflections.getSubTypesOf(UserMapper::class.java)
        .map { it.getConstructor().newInstance() as UserMapper }
    val channelMappers = reflections.getSubTypesOf(ChannelMapper::class.java)
        .map { it.getConstructor().newInstance() as ChannelMapper }
    val roleMappers = reflections.getSubTypesOf(RoleMapper::class.java)
        .map { it.getConstructor().newInstance() as RoleMapper }
    val commandEventMappers = reflections.getSubTypesOf(CommandEventMapper::class.java)
        .map { it.getConstructor().newInstance() as CommandEventMapper }
    val buttonEventMappers = reflections.getSubTypesOf(ButtonEventMapper::class.java)
        .map { it.getConstructor().newInstance() as ButtonEventMapper }
    return Pair(
        commands,
        MapperRegistry(
            stringMappers,
            longMappers,
            booleanMappers,
            userMappers,
            channelMappers,
            roleMappers,
            commandEventMappers,
            buttonEventMappers
        )
    )
}
