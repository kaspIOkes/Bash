package com.czytak.reader

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import java.io.File

/** Wraps MediaPlayer for the synthesized WAV. Speed changes apply live via
 * PlaybackParams, so the audio doesn't need to be re-synthesized when the
 * user drags the rate slider. */
class SpeechController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var pendingSpeed: Float = 1.0f

    fun load(audioBytes: ByteArray) {
        release()
        val file = File(context.cacheDir, "speech.wav")
        file.writeBytes(audioBytes)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            playbackParams = PlaybackParams().setSpeed(pendingSpeed)
        }
    }

    fun setSpeed(speed: Float) {
        pendingSpeed = speed
        mediaPlayer?.let { player ->
            player.playbackParams = player.playbackParams.setSpeed(speed)
        }
    }

    fun play() = mediaPlayer?.start()

    fun pause() = mediaPlayer?.pause()

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun currentPositionSeconds(): Double = (mediaPlayer?.currentPosition ?: 0) / 1000.0

    fun setOnCompletionListener(listener: () -> Unit) {
        mediaPlayer?.setOnCompletionListener { listener() }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
