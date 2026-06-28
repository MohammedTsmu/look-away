package com.eyecare.lookaway.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

/**
 * Pauses media when a break starts and resumes it when it ends, using two
 * complementary mechanisms so it works across as many apps as possible:
 *
 * 1. **MediaSession control** (needs notification-listener access) — precisely
 *    pauses/resumes apps that publish a media session (YouTube, Spotify, most
 *    music players) and lets us resume exactly those.
 * 2. **Audio focus** (no permission needed) — we grab transient-exclusive audio
 *    focus, which makes well-behaved players that *don't* expose a session
 *    (many streaming/movie apps such as Cinemana) pause or duck; releasing it
 *    on resume lets them continue.
 *
 * Some apps with fully custom players honor neither and can't be controlled by
 * any third-party app — that's an OS limitation, not a bug here.
 */
object MediaPauser {

    // Sessions we paused, so we resume only those (not media the user paused).
    private var paused: List<MediaController> = emptyList()
    private var focusRequest: AudioFocusRequest? = null

    /** Whether the user has enabled our notification-listener component. */
    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /**
     * Pauses playback. Grabs audio focus (always) and, if we have access, also
     * pauses active media sessions. Returns how many sessions were paused.
     */
    fun pauseActive(context: Context): Int {
        requestFocus(context)

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

    /** Resumes what we paused: releases audio focus, then plays our sessions. */
    fun resume(context: Context) {
        abandonFocus(context)
        paused.forEach { runCatching { it.transportControls.play() } }
        paused = emptyList()
    }

    /** Drop any held state without resuming (used when stopping for good). */
    fun forget(context: Context) {
        abandonFocus(context)
        paused = emptyList()
    }

    private fun requestFocus(context: Context) {
        if (focusRequest != null) return
        val am = context.getSystemService<AudioManager>() ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(attrs)
            // We don't play audio ourselves; we only hold focus to pause others.
            .setOnAudioFocusChangeListener { }
            .build()
        runCatching { am.requestAudioFocus(request) }
        focusRequest = request
    }

    private fun abandonFocus(context: Context) {
        val request = focusRequest ?: return
        context.getSystemService<AudioManager>()?.let {
            runCatching { it.abandonAudioFocusRequest(request) }
        }
        focusRequest = null
    }
}
