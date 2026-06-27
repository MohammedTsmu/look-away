package com.eyecare.lookaway.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

/**
 * Pauses any media that is *playing* (YouTube, Netflix, Cinemana, podcasts,
 * music, …) when a break starts, and resumes exactly those when it ends.
 *
 * Works through [MediaSessionManager], which requires the user to grant this
 * app notification-listener access (it's how Android exposes other apps' media
 * sessions). Without that access, every call is a safe no-op.
 */
object MediaPauser {

    // Sessions we paused, so we resume only those (not media the user paused).
    private var paused: List<MediaController> = emptyList()

    /** Whether the user has enabled our notification-listener component. */
    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /**
     * Pauses all currently-playing sessions. Returns how many were paused.
     * Remembers them for the next [resume].
     */
    fun pauseActive(context: Context): Int {
        if (!hasAccess(context)) return 0
        val manager = context.getSystemService<MediaSessionManager>() ?: return 0
        val component = ComponentName(context, MediaNotificationListener::class.java)

        val controllers = try {
            manager.getActiveSessions(component)
        } catch (_: SecurityException) {
            return 0
        }

        val playing = controllers.filter {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        playing.forEach { runCatching { it.transportControls.pause() } }
        paused = playing
        return playing.size
    }

    /** Resumes the sessions paused by the most recent [pauseActive]. */
    fun resume(context: Context) {
        if (!hasAccess(context)) {
            paused = emptyList()
            return
        }
        paused.forEach { runCatching { it.transportControls.play() } }
        paused = emptyList()
    }

    fun forget() {
        paused = emptyList()
    }
}
