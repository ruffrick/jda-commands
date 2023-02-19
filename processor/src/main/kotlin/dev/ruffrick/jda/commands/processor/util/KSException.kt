package dev.ruffrick.jda.commands.processor.util

import com.google.devtools.ksp.symbol.KSNode

class KSException(message: String, val node: KSNode) : Exception(message)
