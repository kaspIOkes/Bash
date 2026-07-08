package com.czytak.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Sentence(val words: List<String>, val translation: String)

class ReaderViewModel : ViewModel() {
    var serverUrl by mutableStateOf("http://10.0.2.2:5001")
    var sourceText by mutableStateOf("")
    var translationText by mutableStateOf("")
    var voices by mutableStateOf<List<String>>(emptyList())
    var selectedVoice by mutableStateOf("")
    var rate by mutableStateOf(1.0f)
    var status by mutableStateOf("")
    var isPlaying by mutableStateOf(false)

    var sentences by mutableStateOf<List<Sentence>>(emptyList())
        private set
    var wordSentenceIndex by mutableStateOf<List<Int>>(emptyList())
        private set
    var currentWordIndex by mutableStateOf(-1)
        private set
    var currentSentenceIndex by mutableStateOf(-1)
        private set

    private var timings: List<WordTiming> = emptyList()
    private var lastSynthesizedText: String? = null
    private var speechController: SpeechController? = null
    private var pollJob: Job? = null

    fun attachContext(context: Context) {
        if (speechController == null) {
            speechController = SpeechController(context.applicationContext)
        }
    }

    fun loadVoices() {
        val url = serverUrl
        viewModelScope.launch {
            try {
                val client = TtsApiClient(url)
                val result = withContext(Dispatchers.IO) { client.fetchVoices() }
                voices = result
                if (selectedVoice.isEmpty() || selectedVoice !in result) {
                    selectedVoice = result.firstOrNull() ?: ""
                }
            } catch (e: Exception) {
                status = "Nie można pobrać listy głosów: ${e.message}"
            }
        }
    }

    fun setRate(newRate: Float) {
        rate = newRate
        speechController?.setSpeed(newRate)
    }

    fun togglePlay() {
        val controller = speechController ?: return

        if (controller.isPlaying()) {
            controller.pause()
            isPlaying = false
            pollJob?.cancel()
            return
        }

        val srcSentences = TextUtils.splitSentences(sourceText)
        if (srcSentences.isEmpty()) {
            status = "Wklej najpierw jakiś tekst."
            return
        }
        val translations = translationText.split("\n").map { it.trim() }
        val fullText = srcSentences.joinToString(" ")

        viewModelScope.launch {
            if (fullText != lastSynthesizedText) {
                buildSentences(srcSentences, translations)
                status = "Generuję mowę..."
                try {
                    val client = TtsApiClient(serverUrl)
                    val result = withContext(Dispatchers.IO) { client.speak(fullText, selectedVoice) }
                    timings = result.timings
                    controller.load(result.audioBytes)
                    controller.setSpeed(rate)
                    controller.setOnCompletionListener {
                        isPlaying = false
                        currentWordIndex = -1
                        currentSentenceIndex = -1
                        pollJob?.cancel()
                    }
                    lastSynthesizedText = fullText
                    status = ""
                } catch (e: Exception) {
                    status = "Błąd: ${e.message}"
                    return@launch
                }
            }

            controller.play()
            isPlaying = true
            startPolling(controller)
        }
    }

    private fun buildSentences(srcSentences: List<String>, translations: List<String>) {
        val builtSentences = mutableListOf<Sentence>()
        val indexMap = mutableListOf<Int>()
        srcSentences.forEachIndexed { i, sentence ->
            val words = TextUtils.tokenizeWords(sentence)
            builtSentences.add(Sentence(words, translations.getOrElse(i) { "" }))
            repeat(words.size) { indexMap.add(i) }
        }
        sentences = builtSentences
        wordSentenceIndex = indexMap
        currentWordIndex = -1
        currentSentenceIndex = -1
    }

    private fun startPolling(controller: SpeechController) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive && controller.isPlaying()) {
                val idx = findWordIndexForTime(controller.currentPositionSeconds())
                if (idx != currentWordIndex) {
                    currentWordIndex = idx
                    currentSentenceIndex = if (idx >= 0) wordSentenceIndex.getOrElse(idx) { -1 } else -1
                }
                delay(50)
            }
        }
    }

    private fun findWordIndexForTime(t: Double): Int {
        for (i in timings.indices.reversed()) {
            if (t >= timings[i].start) return i
        }
        return -1
    }

    override fun onCleared() {
        pollJob?.cancel()
        speechController?.release()
    }
}
