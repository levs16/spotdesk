package io.levs.spotdesk

import android.graphics.Bitmap
import android.media.audiofx.Visualizer
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun Offset.normalize(): Offset {
    val length = getDistance()
    return if (length > 0) {
        Offset(x / length, y / length)
    } else {
        this
    }
}

private fun Offset.getDistance(): Float {
    return kotlin.math.sqrt(x * x + y * y)
}

data class DynamicBackgroundState(
    val currentColors: List<Color>,
    val targetColors: List<Color>,
    val transitionProgress: Float,
    val audioData: FloatArray = FloatArray(128) { 0f },
    val time: Float = 0f,
    var bassHistory: MutableList<Float> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DynamicBackgroundState

        if (currentColors != other.currentColors) return false
        if (targetColors != other.targetColors) return false
        if (transitionProgress != other.transitionProgress) return false
        if (!audioData.contentEquals(other.audioData)) return false
        if (time != other.time) return false
        if (bassHistory != other.bassHistory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentColors.hashCode()
        result = 31 * result + targetColors.hashCode()
        result = 31 * result + transitionProgress.hashCode()
        result = 31 * result + audioData.contentHashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + bassHistory.hashCode()
        return result
    }
}

@Composable
fun rememberDynamicBackground(
    bitmap: Bitmap?,
    isPlaying: Boolean,
    audioSessionId: Int
): State<DynamicBackgroundState> {
    var audioData by remember { mutableStateOf(FloatArray(128) { 0f }) }
    var time by remember { mutableStateOf(0f) }
    var bassHistory by remember { mutableStateOf(mutableListOf<Float>()) }

    // Color transition states
    var currentColors by remember {
        mutableStateOf(listOf(Color.DarkGray, Color.Black, Color.DarkGray))
    }
    var targetColors by remember {
        mutableStateOf(listOf(Color.DarkGray, Color.Black, Color.DarkGray))
    }
    var transitionProgress by remember { mutableStateOf(1f) }

    // Extract colors from bitmap
    val newColors = remember(bitmap) {
        if (bitmap != null) {
            val palette = Palette.from(bitmap).generate()
            listOf(
                Color(palette.getDominantColor(Color.DarkGray.toArgb())),
                Color(palette.getDarkMutedColor(Color.Black.toArgb())),
                Color(palette.getDarkVibrantColor(Color.DarkGray.toArgb()))
            )
        } else {
            listOf(Color.DarkGray, Color.Black, Color.DarkGray)
        }
    }

    // Handle color transitions
    LaunchedEffect(newColors) {
        // Only start new transition if colors actually changed
        if (newColors != targetColors) {
            currentColors = if (transitionProgress == 1f) newColors else targetColors
            targetColors = newColors
            transitionProgress = 0f

            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = LinearEasing // Using linear for smoother color transitions
                )
            ) { value, _ ->
                transitionProgress = value
            }
        }
    }

    // Updated visualizer setup with maximum capture rate
    LaunchedEffect(audioSessionId) {
        if (audioSessionId != -1) {
            try {
                val visualizer = Visualizer(audioSessionId).apply {
                    enabled = false
                    captureSize = 512 // Smaller capture size for better performance
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer,
                                waveform: ByteArray,
                                samplingRate: Int
                            ) {
                                // Simplified processing
                                val newData = FloatArray(128) { i ->
                                    val byte = waveform[i * 4]
                                    ((byte.toInt() and 0xFF) / 128f - 1f) * 3f
                                }
                                audioData = newData

                                // Quick bass detection
                                val bassSample = newData.take(4).maxOf { abs(it) }
                                if (bassHistory.size > 2) bassHistory.removeAt(0)
                                bassHistory.add(bassSample)
                            }

                            override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {}
                        },
                        Visualizer.getMaxCaptureRate(),
                        true,
                        false
                    )
                    enabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Slower time updates for smoother animation
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            time += 0.005f
        }
    }

    return remember(currentColors, targetColors, transitionProgress, audioData, time, bassHistory) {
        derivedStateOf {
            DynamicBackgroundState(
                currentColors = currentColors,
                targetColors = targetColors,
                transitionProgress = transitionProgress,
                audioData = audioData,
                time = time,
                bassHistory = bassHistory
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

fun Modifier.dynamicBackground(state: DynamicBackgroundState) = this.drawBehind {
    val currentBass = state.audioData.take(4).maxOf { abs(it) }
    val bassImpact = if (state.bassHistory.size > 1) {
        val change = currentBass - state.bassHistory.last()
        if (change > 0.02f) change * 35f else 0f
    } else 0f

    // Simple linear color interpolation
    fun getColor(index: Int): Color {
        val current = state.currentColors[index]
        val target = state.targetColors[index]
        return Color(
            red = lerp(current.red, target.red, state.transitionProgress),
            green = lerp(current.green, target.green, state.transitionProgress),
            blue = lerp(current.blue, target.blue, state.transitionProgress),
            alpha = lerp(current.alpha, target.alpha, state.transitionProgress)
        )
    }

    val primaryCenter = Offset(
        x = center.x + cos(state.time * 0.3f) * size.width * 0.4f + sin(state.time * 0.2f) * size.width * 0.2f,
        y = center.y + sin(state.time * 0.25f) * size.height * 0.4f + cos(state.time * 0.15f) * size.height * 0.2f
    )

    // Main gradient
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                getColor(0).copy(alpha = 0.8f + abs(bassImpact) * 0.4f),
                getColor(1).copy(alpha = 0.6f + abs(bassImpact) * 0.2f),
                getColor(2).copy(alpha = 0.7f + abs(bassImpact) * 0.3f)
            ),
            center = primaryCenter,
            radius = size.maxDimension * (0.9f + abs(bassImpact) * 0.6f)
        ),
        blendMode = BlendMode.Screen
    )

    // Secondary gradients with smoother transitions
    repeat(3) { index ->
        val offset = index * (PI.toFloat() / 3f)
        val speed = 0.2f + index * 0.1f

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    getColor(index % 3).copy(
                        alpha = (0.4f + abs(bassImpact) * 0.3f) * (1f - index * 0.2f)
                    ),
                    getColor((index + 1) % 3).copy(
                        alpha = (0.3f + abs(bassImpact) * 0.2f) * (1f - index * 0.2f)
                    ),
                    Color.Transparent
                ),
                center = Offset(
                    x = center.x + sin(state.time * speed + offset) * size.width * 0.4f,
                    y = center.y + cos(state.time * speed * 0.7f + offset) * size.height * 0.4f
                ),
                radius = size.maxDimension * (0.7f + abs(bassImpact) * 0.4f + sin(state.time * 0.2f + offset) * 0.1f)
            ),
            blendMode = BlendMode.Screen
        )
    }

    // Beat effects with interpolated colors
    if (bassImpact > 0.1f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    getColor(0).copy(alpha = 0.3f * bassImpact),
                    getColor(1).copy(alpha = 0.2f * bassImpact),
                    Color.Transparent
                ),
                center = primaryCenter,
                radius = size.maxDimension * (0.6f + abs(bassImpact) * 0.8f)
            ),
            blendMode = BlendMode.Screen
        )
    }

    if (bassImpact > 0.15f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f * bassImpact),
                    Color.White.copy(alpha = 0.1f * bassImpact),
                    Color.Transparent
                ),
                center = Offset(
                    x = center.x + (primaryCenter.x - center.x) * 0.5f,
                    y = center.y + (primaryCenter.y - center.y) * 0.5f
                ),
                radius = size.maxDimension * (0.4f + abs(bassImpact) * 0.6f)
            ),
            blendMode = BlendMode.Screen
        )
    }
}

private const val PI = Math.PI