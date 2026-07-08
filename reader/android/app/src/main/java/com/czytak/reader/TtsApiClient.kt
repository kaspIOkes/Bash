package com.czytak.reader

import android.util.Base64
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class WordTiming(val word: String, val start: Double, val end: Double)
data class SpeakResult(val audioBytes: ByteArray, val timings: List<WordTiming>)

/** Talks to the same Flask backend (reader/server) used by the web prototype. */
class TtsApiClient(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetchVoices(): List<String> {
        val request = Request.Builder().url("$baseUrl/api/voices").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val array = JSONArray(body)
            return List(array.length()) { array.getString(it) }
        }
    }

    fun speak(text: String, voice: String): SpeakResult {
        val json = JSONObject().apply {
            put("text", text)
            put("voice", voice)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$baseUrl/api/speak").post(body).build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw IOException("Pusta odpowiedź")
            if (!response.isSuccessful) {
                val error = runCatching { JSONObject(responseBody).optString("error") }.getOrNull()
                throw IOException(error ?: "HTTP ${response.code}")
            }

            val obj = JSONObject(responseBody)
            val audioBytes = Base64.decode(obj.getString("audio_base64"), Base64.DEFAULT)
            val timingsArray = obj.getJSONArray("timings")
            val timings = List(timingsArray.length()) { i ->
                val t = timingsArray.getJSONObject(i)
                WordTiming(t.getString("word"), t.getDouble("start"), t.getDouble("end"))
            }
            return SpeakResult(audioBytes, timings)
        }
    }
}
