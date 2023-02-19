package dev.ruffrick.jda.commands.processor.util

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

data class KSTypes(
    val stringType: KSType,
    val longType: KSType,
    val intType: KSType,
    val booleanType: KSType,
    val doubleType: KSType,
    val userType: KSType,
    val channelType: KSType,
    val roleType: KSType,
    val mentionableType: KSType,
    val attachmentType: KSType,
    val commandEventType: KSType,
    val buttonEventType: KSType
) {
    constructor(resolver: Resolver) : this(
        resolver.builtIns.stringType,
        resolver.builtIns.longType,
        resolver.builtIns.intType,
        resolver.builtIns.booleanType,
        resolver.builtIns.doubleType,
        resolver.getClassDeclaration<User>(),
        resolver.getClassDeclaration<Channel>(),
        resolver.getClassDeclaration<Role>(),
        resolver.getClassDeclaration<IMentionable>(),
        resolver.getClassDeclaration<Message.Attachment>(),
        resolver.getClassDeclaration<SlashCommandInteractionEvent>(),
        resolver.getClassDeclaration<ButtonInteractionEvent>()
    )
}
