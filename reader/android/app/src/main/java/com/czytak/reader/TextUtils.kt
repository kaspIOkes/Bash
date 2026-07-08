package com.czytak.reader

/** Same sentence/word splitting rules as the web prototype's app.js, so word
 * order lines up 1:1 with the server's timings for a given piece of text. */
object TextUtils {
    private val sentenceRegex = Regex("(?<=[.!?])\\s+")
    private val wordRegex = Regex("\\S+")

    fun splitSentences(text: String): List<String> =
        text.split(sentenceRegex).map { it.trim() }.filter { it.isNotEmpty() }

    fun tokenizeWords(sentence: String): List<String> =
        wordRegex.findAll(sentence).map { it.value }.toList()
}
