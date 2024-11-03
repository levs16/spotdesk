package io.levs.spotdesk

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlin.math.abs

class BeatDetector {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private val energyHistory = ArrayDeque<Float>(8)
    private val beatThreshold = 1.5f

    init {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startBeatDetection(): Flow<Float> = flow {
        val buffer = ShortArray(bufferSize)
        audioRecord?.startRecording()

        try {
            while (true) {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    val energy = calculateEnergy(buffer, readSize)
                    val beatIntensity = detectBeat(energy)
                    if (beatIntensity > 0) {
                        emit(beatIntensity)
                    }
                }
                delay(16)
            }
        } finally {
            audioRecord?.stop()
        }
    }

    private fun calculateEnergy(buffer: ShortArray, size: Int): Float {
        var sum = 0f
        for (i in 0 until size) {
            sum += abs(buffer[i].toFloat())
        }
        return sum / size
    }

    private fun detectBeat(energy: Float): Float {
        energyHistory.add(energy)
        if (energyHistory.size > 8) {
            energyHistory.removeFirst()
        }

        val average = energyHistory.average().toFloat()
        return if (energy > average * beatThreshold) {
            ((energy / average) - beatThreshold).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    fun release() {
        audioRecord?.release()
        audioRecord = null
    }
}