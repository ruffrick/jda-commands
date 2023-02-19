package dev.ruffrick.jda.commands.mapping

import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

interface Mapper<S, T> {

    suspend fun transform(value: S): T

}

interface StringMapper<T> : Mapper<String, T>

interface LongMapper<T> : Mapper<Long, T>

interface BooleanMapper<T> : Mapper<Boolean, T>

interface UserMapper<T> : Mapper<User, T>

interface ChannelMapper<T> : Mapper<GuildChannel, T>

interface RoleMapper<T> : Mapper<Role, T>

interface CommandEventMapper<T> : Mapper<SlashCommandEvent, T>

interface ButtonEventMapper<T> : Mapper<ButtonClickEvent, T>
