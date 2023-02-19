package dev.ruffrick.jda.commands.processor

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.ruffrick.jda.commands.api.SlashCommand
import dev.ruffrick.jda.commands.api.annotation.*
import dev.ruffrick.jda.commands.processor.util.KSException
import dev.ruffrick.jda.commands.processor.util.KSTypes
import dev.ruffrick.jda.commands.processor.util.hyphenate
import dev.ruffrick.jda.commands.processor.visitor.CommandVisitor

class CommandProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private val indent = " ".repeat(4)

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val types = KSTypes(resolver)
        val commands = mutableListOf<ClassName>()
        val sources = mutableListOf<KSFile>()
        resolver.getSymbolsWithAnnotation(Command::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .forEach {
                try {
                    if (!it.getAllSuperTypes().contains(types.slashCommandType)) {
                        throw KSException("Command class must extend SlashCommand", it)
                    }
                    val command = it.getAnnotationsByType(Command::class).first()
                    val name = command.name.lowercase()
                        .ifEmpty { it.simpleName.asString().removeSuffix("Command").hyphenate() }
                    val description = command.description.ifEmpty { "The $name command" }

                    val className = "${it.simpleName.asString()}Impl"
                    val spec = it.accept(CommandVisitor(types), CommandVisitor.Data(name, description))
                    val commandType = TypeSpec.classBuilder(className)
                        .superclass(it.asType(emptyList()).toTypeName())
                        .addProperty(spec.commandData)
                        .addFunction(spec.onSlashCommandInteraction)
                        .addFunction(spec.onButtonInteraction)
                        .build()
                    val packageName = it.packageName.asString()
                    val source = it.containingFile!!
                    FileSpec.builder(packageName, className)
                        .indent(indent)
                        .addType(commandType)
                        .build()
                        .writeTo(codeGenerator, Dependencies(false, source))
                    commands.add(ClassName(packageName, className))
                    sources.add(source)
                } catch (e: KSException) {
                    logger.error(e.message!!, e.node)
                }
            }

        if (commands.isNotEmpty()) {
            val initializer = CodeBlock.builder().addStatement("listOf(").indent()
            commands.forEach { initializer.addStatement("%T(),", it) }
            initializer.unindent().addStatement(")")
            val registryType = TypeSpec.classBuilder("CommandRegistry")
                .addProperty(
                    PropertySpec.builder("commands", List::class.parameterizedBy(SlashCommand::class))
                        .initializer(initializer.build())
                        .build()
                )
                .addFunction(
                    FunSpec.builder("getInstance")
                        .addModifiers(KModifier.INLINE)
                        .addTypeVariable(TypeVariableName.invoke("reified T", SlashCommand::class))
                        .addStatement("return commands.first { it is T }")
                        .build()
                )
                .build()
            FileSpec.builder("", "CommandRegistry")
                .indent(indent)
                .addType(registryType)
                .build()
                .writeTo(codeGenerator, Dependencies(true, *sources.toTypedArray()))
        }

        return emptyList()
    }
}
