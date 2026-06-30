package com.eyecare.lookaway.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import com.eyecare.lookaway.R
import com.eyecare.lookaway.util.LocaleManager

/**
 * A calming full-screen reminder shown while the user is inside an app that has
 * passed its daily limit. Not a block — a strong, dismissible nudge with the
 * app's icon, how far over the limit you are, snooze options, a one-tap "leave
 * app", and a per-visit Dismiss. Drawn as an overlay window (needs "Display over
 * other apps").
 */
object LimitOverlay {

    private val snoozeOptions = intArrayOf(5, 15, 30)

    private var root: View? = null
    private var pkg: String? = null

    fun isShowing(): Boolean = root != null
    fun showingPackage(): String? = pkg

    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    fun show(
        context: Context,
        packageName: String,
        appLabel: String,
        usedText: String,
        overText: String,
        dismissNote: String?,
        onSnooze: (Int) -> Unit,
        onLeave: () -> Unit,
        onDismiss: () -> Unit,
    ): Boolean {
        if (root != null) return true
        val wm = context.getSystemService<WindowManager>() ?: return false
        val l = android.view.ContextThemeWrapper(LocaleManager.wrap(context), R.style.Theme_LookAway)
        val dp = context.resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val column = LinearLayout(l).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(px(28), px(28), px(28), px(28))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0E3B3A"), Color.parseColor("#05100F")),
            )
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        }

        fun text(value: String, sizeSp: Float, bold: Boolean, alpha: Int = 255) = TextView(l).apply {
            text = value
            setTextColor(Color.argb(alpha, 255, 255, 255))
            textSize = sizeSp
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val icon = ImageView(l).apply {
            setImageDrawable(
                runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull(),
            )
        }
        column.addView(
            icon,
            LinearLayout.LayoutParams(px(64), px(64)).apply { bottomMargin = px(12) },
        )

        column.addView(text(l.getString(R.string.app_limit_nudge_title), 14f, false, 200))
        column.addView(text(appLabel, 28f, true).apply { setPadding(0, px(4), 0, px(4)) })
        column.addView(text(usedText, 15f, false, 220))
        column.addView(text(overText, 15f, false, 220).apply { setPadding(0, px(2), 0, px(if (dismissNote != null) 4 else 24)) })
        if (dismissNote != null) {
            column.addView(text(dismissNote, 13f, true, 235).apply {
                setTextColor(Color.parseColor("#FFD9A0"))
                setPadding(0, px(4), 0, px(24))
            })
        }

        fun pill(label: String, filled: Boolean, onClick: () -> Unit) = TextView(l).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = px(24).toFloat()
                setColor(if (filled) Color.argb(60, 255, 255, 255) else Color.argb(26, 255, 255, 255))
            }
            setPadding(px(22), px(10), px(22), px(10))
            isClickable = true
            setOnClickListener { onClick() }
        }

        // Snooze options row (5 / 15 / 30 min).
        column.addView(text(l.getString(R.string.limit_snooze_label), 13f, false, 180).apply {
            setPadding(0, 0, 0, px(6))
        })
        val snoozeRow = LinearLayout(l).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        snoozeOptions.forEach { minutes ->
            snoozeRow.addView(
                pill(l.getString(R.string.minutes_short, minutes), false) { onSnooze(minutes) },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = px(5); marginEnd = px(5) },
            )
        }
        column.addView(snoozeRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = px(16) })

        column.addView(
            pill(l.getString(R.string.limit_leave), true) { onLeave() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = px(10) },
        )
        column.addView(
            pill(l.getString(R.string.limit_dismiss), false) { onDismiss() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER_HORIZONTAL },
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        return runCatching {
            wm.addView(column, params)
            root = column
            pkg = packageName
            true
        }.getOrElse { root = null; pkg = null; false }
    }

    fun hide(context: Context) {
        val current = root ?: return
        context.getSystemService<WindowManager>()?.let { wm -> runCatching { wm.removeView(current) } }
        root = null
        pkg = null
    }
}
