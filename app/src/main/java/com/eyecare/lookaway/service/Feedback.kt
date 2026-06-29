package com.eyecare.lookaway.service

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/** Plays the break chime + a vibration pattern, honoring the user's toggles. */
object Feedback {

    fun playBreakStart(context: Context, sound: Boolean, vibrate: Boolean, soundUri: String = "") {
        if (sound) playChime(context, soundUri)
        if (vibrate) vibrate(context, longArrayOf(0, 220, 120, 220))
    }

    fun playBreakEnd(context: Context, sound: Boolean, vibrate: Boolean, soundUri: String = "") {
        if (vibrate) vibrate(context, longArrayOf(0, 120))
        if (sound) playChime(context, soundUri)
    }

    private fun playChime(context: Context, soundUri: String) {
        runCatching {
            val uri = if (soundUri.isBlank()) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                android.net.Uri.parse(soundUri)
            }
            RingtoneManager.getRingtone(context, uri)?.play()
        }
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
}
