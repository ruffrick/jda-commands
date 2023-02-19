package dev.ruffrick.jda.commands.processor.util

fun String.hyphenate(): String {
    val word = StringBuilder()
    val builder = StringBuilder()
    for (index in indices) {
        val char = this[index]
        if (index > 0 && char.isUpperCase()) {
            builder.append(word).append('-')
            word.clear()
        }
        word.append(char.lowercase())
    }
    builder.append(word)
    return builder.toString()
}
