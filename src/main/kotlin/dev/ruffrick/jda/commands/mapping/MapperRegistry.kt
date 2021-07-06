package dev.ruffrick.jda.commands.mapping

import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions

class MapperRegistry(
    stringMappers: List<StringMapper<*>> = listOf(),
    longMappers: List<LongMapper<*>> = listOf(),
    booleanMappers: List<BooleanMapper<*>> = listOf(),
    userMappers: List<UserMapper<*>> = listOf(),
    channelMappers: List<ChannelMapper<*>> = listOf(),
    roleMappers: List<RoleMapper<*>> = listOf(),
    commandEventMappers: List<CommandEventMapper<*>> = listOf(),
    buttonEventMappers: List<ButtonEventMapper<*>> = listOf()
) {

    val stringMappers = stringMappers.associateBy { returnType(it) }
    val longMappers = longMappers.associateBy { returnType(it) }
    val booleanMappers = booleanMappers.associateBy { returnType(it) }
    val userMappers = userMappers.associateBy { returnType(it) }
    val channelMappers = channelMappers.associateBy { returnType(it) }
    val roleMappers = roleMappers.associateBy { returnType(it) }
    val commandEventMappers = commandEventMappers.associateBy { returnType(it) }
    val buttonEventMappers = buttonEventMappers.associateBy { returnType(it) }

    private fun <S, T> returnType(mapper: Mapper<S, T>) =
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>

}
