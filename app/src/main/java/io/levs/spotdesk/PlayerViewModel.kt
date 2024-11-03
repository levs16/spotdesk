package io.levs.spotdesk

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.os.SystemClock
import kotlinx.coroutines.Job
import android.media.AudioManager

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtUri: String? = null,
    val audioSessionId: Int = -1,
    val customActions: List<CustomAction> = emptyList()
)

data class CustomAction(
    val icon: Int,  // Resource ID for the icon
    val title: String,
    val action: String
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaController: MediaController? = null
    private val mediaSessionManager = application.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val componentName = ComponentName(application, MediaNotificationListener::class.java)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _albumArtBitmap = MutableStateFlow<Bitmap?>(null)
    val albumArtBitmap: StateFlow<Bitmap?> = _albumArtBitmap.asStateFlow()

    private val _audioSessionId = MutableStateFlow(audioManager.generateAudioSessionId())
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            updatePlayerState()
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            super.onMetadataChanged(metadata)
            updatePlayerState()
        }
    }

    private var progressUpdateJob: Job? = null
    private var seekJob: Job? = null
    private var lastSeekTime = 0L

    init {
        viewModelScope.launch {
            while(true) {
                try {
                    val sessions = mediaSessionManager.getActiveSessions(componentName)
                    if (sessions.isNotEmpty()) {
                        val controller = sessions[0]
                        mediaController?.unregisterCallback(mediaCallback)
                        mediaController = controller
                        controller.registerCallback(mediaCallback)
                        updatePlayerState()
                    }
                } catch (e: SecurityException) {
                    Log.e("PlayerViewModel", "Permission denied: ${e.message}")
                }
                delay(1000)
            }
        }
        startProgressUpdates()
    }

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while(true) {
                updateProgress()
                delay(16) // ~60fps update rate
            }
        }
    }

    private fun updateProgress() {
        val controller = mediaController ?: return
        val playbackState = controller.playbackState ?: return

        if (playbackState.state == PlaybackState.STATE_PLAYING) {
            val realPosition = playbackState.position +
                    ((SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime) *
                            playbackState.playbackSpeed).toLong()

            _playerState.value = _playerState.value.copy(currentPosition = realPosition)
        }
    }

    private fun updatePlayerState() {
        val controller = mediaController ?: return

        // Get the global audio session ID
        val sessionId = audioManager.generateAudioSessionId()
        _audioSessionId.value = sessionId

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        // Get album art bitmap directly
        val newBitmap = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)

        if (newBitmap != _albumArtBitmap.value) {
            _albumArtBitmap.value = newBitmap
        }

        _playerState.value = PlayerState(
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
            currentPosition = playbackState?.position ?: 0L,
            duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            album = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            audioSessionId = sessionId
        )
    }

    private fun saveBitmapToCache(bitmap: Bitmap): String {
        val file = File(getApplication<Application>().cacheDir, "album_art.jpg")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            return Uri.fromFile(file).toString()
        } catch (e: IOException) {
            Log.e("PlayerViewModel", "Failed to save album art: ${e.message}")
            return ""
        }
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (playerState.value.isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipToNext() {
        mediaController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun seekTo(position: Long) {
        val now = System.currentTimeMillis()
        // Prevent too frequent seek operations
        if (now - lastSeekTime < 16) return // ~60fps limit

        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            // Update UI immediately for smooth feeling
            _playerState.value = _playerState.value.copy(currentPosition = position)

            // Debounce actual media seeking
            delay(32) // Additional debounce for smoother experience
            mediaController?.transportControls?.seekTo(position)
            lastSeekTime = now
        }
    }

    override fun onCleared() {
        super.onCleared()
        seekJob?.cancel()
    }
}