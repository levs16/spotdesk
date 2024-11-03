package io.levs.spotdesk

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlayerService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            // Set default player behaviors
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.release()
            release()
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
}