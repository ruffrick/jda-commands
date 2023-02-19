package dev.ruffrick.jda.commands.processor.visitor

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.CodeBlock
import dev.ruffrick.jda.commands.processor.util.KSException
import dev.ruffrick.jda.commands.processor.util.KSTypes
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class OptionVisitor(private val types: KSTypes) : KSDefaultVisitor<OptionVisitor.Data, OptionVisitor.Spec>() {
    override fun visitValueParameter(valueParameter: KSValueParameter, data: Data): Spec {
        val type = valueParameter.type.resolve()
        val optionType = when (type.makeNotNullable()) {
            types.stringType -> Pair("STRING", "asString")
            types.longType -> Pair("INTEGER", "asLong")
            types.intType -> Pair("INTEGER", "asInt")
            types.booleanType -> Pair("BOOLEAN", "asBoolean")
            types.userType -> Pair("USER", "asUser")
            types.channelType -> Pair("CHANNEL", "asChannel")
            types.roleType -> Pair("ROLE", "asRole")
            types.mentionableType -> Pair("MENTIONABLE", "asMentionable")
            types.doubleType -> Pair("NUMBER", "asDouble")
            types.attachmentType -> Pair("ATTACHMENT", "asAttachment")
            else -> throw KSException("Invalid option type $type", valueParameter)
        }
        val required = !type.isMarkedNullable
        val optionData = CodeBlock.of(
            "%T(%T.%L, %S, %S, %L),\n",
            OptionData::class,
            OptionType::class,
            optionType.first,
            data.name,
            data.description,
            required
        )
        val invoke =
            CodeBlock.of("event.getOption(%S)%L.%L,", data.name, if (required) "!!" else "?", optionType.second)
        return Spec(optionData, invoke)
    }

    override fun defaultHandler(node: KSNode, data: Data): Spec {
        throw KSException("Unexpected call to defaultHandler()", node)
    }

    data class Data(val name: String, val description: String)

    data class Spec(val optionData: CodeBlock, val invoke: CodeBlock)
}
