package dev.ruffrick.jda.commands.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.*
import dev.ruffrick.jda.commands.api.annotation.Button
import dev.ruffrick.jda.commands.api.annotation.Entrypoint
import dev.ruffrick.jda.commands.api.annotation.Permissions
import dev.ruffrick.jda.commands.api.annotation.Subcommand
import dev.ruffrick.jda.commands.processor.util.KSException
import dev.ruffrick.jda.commands.processor.util.KSTypes
import dev.ruffrick.jda.commands.processor.util.hyphenate
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

class CommandVisitor(private val types: KSTypes) : KSDefaultVisitor<CommandVisitor.Data, CommandVisitor.Spec>() {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Data): Spec {
        val commandData = CodeBlock.builder()
            .addStatement("%T.slash(%S, %S)", Commands::class, data.name, data.description).indent()
        val onSlashCommandInteraction = FunSpec.builder("onSlashCommandInteraction")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("event", SlashCommandInteractionEvent::class)
            .beginControlFlow("if (event.name != %S)", data.name)
            .addStatement("return")
            .endControlFlow()
        val onButtonInteraction = FunSpec.builder("onButtonInteraction")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("event", ButtonInteractionEvent::class)

        val candidates = classDeclaration.getDeclaredFunctions().map {
            Candidate(
                it,
                it.getAnnotationsByType(Entrypoint::class).firstOrNull(),
                it.getAnnotationsByType(Subcommand::class).firstOrNull(),
                it.getAnnotationsByType(Button::class).firstOrNull()
            )
        }

        if (candidates.any { it.entrypoint != null } && candidates.any { it.subcommand != null }) {
            throw KSException("Cannot mix top-level commands and subcommands", classDeclaration)
        }

        if (candidates.count { it.entrypoint != null } > 1) {
            throw KSException("Cannot have more than one @Entrypoint", classDeclaration)
        }

        val subcommands = mutableListOf<SubcommandVisitor.Spec>()
        candidates.forEach {
            when {
                it.entrypoint != null && it.button != null ->
                    throw KSException("Cannot mix @Entrypoint and @Button", it.function)

                it.subcommand != null && it.button != null ->
                    throw KSException("Cannot mix @Subcommand and @Button", it.function)

                it.entrypoint != null -> {
                    val spec = it.function.accept(EntrypointVisitor(types), EntrypointVisitor.Data(Unit))
                    commandData.add(spec.options)
                    onSlashCommandInteraction.addCode(spec.invoke)
                }

                it.subcommand != null -> {
                    val name = it.subcommand.name.ifEmpty { it.function.simpleName.asString().hyphenate() }
                    val description = it.subcommand.description.ifEmpty { "The $name subcommand" }
                    subcommands.add(
                        it.function.accept(SubcommandVisitor(types), SubcommandVisitor.Data(name, description))
                    )
                }

                it.button != null -> {
                    TODO()
                }
            }
        }

        if (subcommands.isNotEmpty()) {
            commandData.addStatement(".addSubcommands(").indent()
            onSlashCommandInteraction.beginControlFlow("when (event.subcommandName)")
            subcommands.forEach {
                commandData.add(it.subcommandData)
                onSlashCommandInteraction.addCode(it.invoke)
            }
            commandData.unindent().addStatement(")")
            onSlashCommandInteraction.endControlFlow()
        }

        classDeclaration.getAnnotationsByType(Permissions::class).firstOrNull()?.let { permissions ->
            commandData.addStatement(".setDefaultPermissions(").indent()
                .addStatement("%T.enabledFor(", DefaultMemberPermissions::class).indent()
            permissions.value.forEach { commandData.addStatement("%T.%L, ", Permission::class, it.name) }
            commandData.unindent().addStatement(")").unindent().addStatement(")")
        }

        return Spec(
            PropertySpec.builder("commandData", CommandData::class, KModifier.OVERRIDE)
                .initializer(commandData.unindent().build()).build(),
            onSlashCommandInteraction.build(),
            onButtonInteraction.build()
        )
    }

    override fun defaultHandler(node: KSNode, data: Data): Spec {
        throw KSException("Unexpected call to defaultHandler()", node)
    }

    data class Data(val name: String, val description: String)

    data class Spec(
        val commandData: PropertySpec,
        val onSlashCommandInteraction: FunSpec,
        val onButtonInteraction: FunSpec
    )

    private data class Candidate(
        val function: KSFunctionDeclaration,
        val entrypoint: Entrypoint?,
        val subcommand: Subcommand?,
        val button: Button?
    )
}
