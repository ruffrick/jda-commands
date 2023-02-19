package dev.ruffrick.jda.commands.processor.util

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver

inline fun <reified T : Any> Resolver.getClassDeclaration() =
    getClassDeclarationByName(T::class.qualifiedName!!)!!.asType(emptyList())
