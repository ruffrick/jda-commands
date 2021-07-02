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

    val stringMappers = stringMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val longMappers = longMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val booleanMappers = booleanMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val userMappers = userMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val channelMappers = channelMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val roleMappers = roleMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val commandEventMappers = commandEventMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }
    val buttonEventMappers = buttonEventMappers.associateBy { mapper ->
        mapper::class.memberFunctions.first { it.name == "transform" }.returnType.classifier as KClass<*>
    }

}
