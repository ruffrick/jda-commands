package dev.ruffrick.jda.commands

import dev.ruffrick.jda.commands.mapping.Mapper

class CommandRegistryBuilder {
    private var commands: MutableList<SlashCommand> = mutableListOf()
    private var mappers: MutableList<Mapper<Any, Any?>> = mutableListOf()

    fun addCommands(commands: Collection<SlashCommand>): CommandRegistryBuilder {
        this.commands.addAll(commands)
        return this
    }

    fun addCommands(vararg commands: SlashCommand): CommandRegistryBuilder {
        this.commands.addAll(commands)
        return this
    }

    fun addCommand(command: SlashCommand): CommandRegistryBuilder {
        this.commands.add(command)
        return this
    }

    fun setCommands(commands: Collection<SlashCommand>): CommandRegistryBuilder {
        this.commands = commands.toMutableList()
        return this
    }

    fun setCommands(vararg commands: SlashCommand): CommandRegistryBuilder {
        this.commands = commands.toMutableList()
        return this
    }

    fun addMappers(mappers: Collection<Mapper<Any, Any?>>): CommandRegistryBuilder {
        this.mappers.addAll(mappers)
        return this
    }

    fun addMappers(vararg mappers: Mapper<Any, Any?>): CommandRegistryBuilder {
        this.mappers.addAll(mappers)
        return this
    }

    fun <S, T> addMapper(mapper: Mapper<S, T>): CommandRegistryBuilder {
        @Suppress("unchecked_cast")
        this.mappers.add(mapper as Mapper<Any, Any?>)
        return this
    }

    fun setMappers(mappers: Collection<Mapper<Any, Any?>>): CommandRegistryBuilder {
        this.mappers = mappers.toMutableList()
        return this
    }

    fun setMappers(vararg mappers: Mapper<Any, Any?>): CommandRegistryBuilder {
        this.mappers = mappers.toMutableList()
        return this
    }

    fun build(): CommandRegistry {
        require(commands.isNotEmpty()) { "Commands must not be empty" }
        return CommandRegistry(commands, mappers)
    }
}
