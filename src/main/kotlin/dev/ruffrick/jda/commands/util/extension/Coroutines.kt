package dev.ruffrick.jda.commands.util.extension

import kotlinx.coroutines.CompletableDeferred
import net.dv8tion.jda.api.requests.RestAction

suspend inline fun <T> RestAction<T>.await() = completeAsync().await()

fun <T> RestAction<T>.completeAsync() = CompletableDeferred<T?>().apply {
    queue(
        { complete(it) },
        { complete(null) }
    )
}
