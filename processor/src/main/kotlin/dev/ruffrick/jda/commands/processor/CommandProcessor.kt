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
import dev.ruffrick.jda.commands.processor.util.getClassDeclaration
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

        val symbols = resolver.getSymbolsWithAnnotation(Command::class.qualifiedName!!).toList()
        val invalid = symbols.filter { !it.validate() }
        val containers = symbols.filterIsInstance<KSClassDeclaration>().filter { it.validate() }
        if (containers.isEmpty()) {
            return invalid
        }

        val commands = CodeBlock.builder().addStatement("listOf(").indent()

        val slashCommandType = resolver.getClassDeclaration<SlashCommand>()
        try {
            containers.forEach { declaration ->
                if (!declaration.getAllSuperTypes().contains(slashCommandType)) {
                    throw KSException("Command class must extend SlashCommand", declaration)
                }
                val command = declaration.getAnnotationsByType(Command::class).first()
                val name = command.name.lowercase()
                    .ifEmpty { declaration.simpleName.asString().removeSuffix("Command").hyphenate() }
                val description = command.description.ifEmpty { "The $name command" }

                val className = "${declaration.simpleName.asString()}Impl"
                val spec = declaration.accept(CommandVisitor(types), CommandVisitor.Data(name, description))
                val commandType = TypeSpec.classBuilder(className)
                    .superclass(declaration.asType(emptyList()).toTypeName())
                    .addProperty(spec.commandData)
                    .addFunction(spec.onSlashCommandInteraction)
                    .addFunction(spec.onButtonInteraction)
                    .build()
                FileSpec.builder(declaration.packageName.asString(), className)
                    .indent(indent)
                    .addType(commandType)
                    .build()
                    .writeTo(codeGenerator, false)

                commands.addStatement("%T(),", ClassName(declaration.packageName.asString(), className))
            }
        } catch (e: KSException) {
            logger.error(e.message!!, e.node)
        }

        val registryType = TypeSpec.classBuilder("CommandRegistry")
            .addProperty(
                PropertySpec.builder("commands", List::class.parameterizedBy(SlashCommand::class))
                    .initializer(commands.unindent().addStatement(")").build())
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
            .writeTo(codeGenerator, false)

        return invalid
    }
}
