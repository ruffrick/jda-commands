package dev.ruffrick.jda.commands.util.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

internal object TaskManager {

    private val coroutineDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    fun async(block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        block()
    }

}
