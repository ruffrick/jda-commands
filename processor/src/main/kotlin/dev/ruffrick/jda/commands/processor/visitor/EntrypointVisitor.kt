package dev.ruffrick.jda.commands.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.CodeBlock
import dev.ruffrick.jda.commands.api.annotation.Option
import dev.ruffrick.jda.commands.processor.util.KSException
import dev.ruffrick.jda.commands.processor.util.KSTypes
import dev.ruffrick.jda.commands.processor.util.hyphenate

class EntrypointVisitor(private val types: KSTypes) :
    KSDefaultVisitor<EntrypointVisitor.Data, EntrypointVisitor.Spec>() {
    @OptIn(KspExperimental::class)
    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Data): Spec {
        val options = CodeBlock.builder().addStatement(".addOptions(").indent()
        val invoke = CodeBlock.builder().addStatement("this.%L(", function).indent()

        function.parameters.forEach {
            val option = it.getAnnotationsByType(Option::class).firstOrNull()
            when {
                option != null -> {
                    val name = option.name.ifEmpty { it.name!!.asString().hyphenate() }
                    val description = option.description.ifEmpty { "The $name option" }
                    val spec = it.accept(OptionVisitor(types), OptionVisitor.Data(name, description))
                    options.add(spec.optionData)
                    invoke.add(spec.invoke)
                }

                it.type.resolve() == types.commandEventType -> invoke.addStatement("event,")
                else -> throw KSException("Cannot use ${it.type} as an argument", it)
            }
        }

        options.unindent().addStatement(")")
        invoke.unindent().addStatement(")")
        return Spec(options.build(), invoke.build())
    }


    override fun defaultHandler(node: KSNode, data: Data): Spec {
        throw KSException("Unexpected call to defaultHandler()", node)
    }

    data class Data(val unit: Unit)

    data class Spec(val options: CodeBlock, val invoke: CodeBlock)
}
