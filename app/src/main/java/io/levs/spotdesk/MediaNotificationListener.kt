package io.levs.spotdesk

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

class MediaNotificationListener : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private lateinit var componentName: ComponentName

    override fun onCreate() {
        super.onCreate()
        componentName = ComponentName(this, MediaNotificationListener::class.java)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MediaListener", "Listener Connected!")

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        try {
            // Get active media sessions
            val sessions = mediaSessionManager?.getActiveSessions(componentName)
            sessions?.forEach { controller ->
                Log.d("MediaListener", "Found media session: ${controller.packageName}")
            }
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Permission denied: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.notification.extras.containsKey("android.mediaSession")) {
            Log.d("MediaListener", "Media notification from: ${sbn.packageName}")

            try {
                val sessions = mediaSessionManager?.getActiveSessions(componentName)
                sessions?.forEach { controller ->
                    Log.d("MediaListener", "Active session: ${controller.packageName}")
                }
            } catch (e: SecurityException) {
                Log.e("MediaListener", "Permission denied: ${e.message}")
            }
        }
    }
}