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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import com.eyecare.lookaway.R
import com.eyecare.lookaway.service.ReminderEngine
import com.eyecare.lookaway.util.LocaleManager

/**
 * Draws the break as a full-screen **overlay window** via WindowManager. Unlike
 * launching an Activity, this is honored even on OEMs that block background
 * activity starts (e.g. MIUI) — as long as "Display over other apps" is granted.
 * Built with plain Views so it can live in a window without an Activity host.
 */
object BreakOverlay {

    private var root: View? = null
    private var ring: CountdownRingView? = null

    fun isShowing(): Boolean = root != null

    /**
     * Adds the overlay window. Returns true only if it was actually added, so
     * the caller can fall back to a notification when the OEM blocks overlays.
     * FLAG_SHOW_WHEN_LOCKED/TURN_SCREEN_ON are deprecated for Activities but are
     * the only way to set them on a non-Activity overlay window.
     */
    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    fun show(context: Context, showSkip: Boolean): Boolean {
        if (root != null) return true
        val wm = context.getSystemService<WindowManager>() ?: return false
        // A themed context so plain widgets resolve their styles reliably.
        val l = android.view.ContextThemeWrapper(
            LocaleManager.wrap(context),
            R.style.Theme_LookAway,
        )
        val dp = context.resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val container = FrameLayout(l).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0E3B3A"), Color.parseColor("#05100F")),
            )
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        }

        val column = LinearLayout(l).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(px(32), px(32), px(32), px(32))
        }

        val headline = TextView(l).apply {
            text = l.getString(R.string.break_headline)
            setTextColor(Color.WHITE)
            textSize = 26f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val sub = TextView(l).apply {
            text = l.getString(R.string.break_sub)
            setTextColor(Color.argb(210, 255, 255, 255))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, px(8), 0, px(24))
        }
        val ringView = CountdownRingView(l).apply {
            layoutParams = LinearLayout.LayoutParams(px(200), px(200))
        }
        ring = ringView

        column.addView(headline)
        column.addView(sub)
        column.addView(ringView)

        if (showSkip) {
            // A TextView styled as a button — no theme/AppCompat dependency.
            val skip = TextView(l).apply {
                text = l.getString(R.string.action_skip)
                setTextColor(Color.WHITE)
                textSize = 15f
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = px(28).toFloat()
                    setColor(Color.argb(40, 255, 255, 255))
                }
                setPadding(px(32), px(12), px(32), px(12))
                isClickable = true
                setOnClickListener { ReminderEngine.endBreak() }
            }
            column.addView(
                skip,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = px(24); gravity = Gravity.CENTER_HORIZONTAL },
            )
        }

        container.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply { gravity = Gravity.CENTER },
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // NOT_FOCUSABLE keeps the window from grabbing input focus, so it
            // doesn't fight the app underneath (which made it flicker / drop
            // behind on some OEMs). Touches on our views (Skip) still work.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        return runCatching {
            wm.addView(container, params)
            root = container
            true
        }.getOrElse {
            root = null
            ring = null
            false
        }
    }

    fun update(seconds: Int, progress: Float) {
        ring?.set(seconds, progress)
    }

    fun hide(context: Context) {
        val current = root ?: return
        context.getSystemService<WindowManager>()?.let { wm ->
            runCatching { wm.removeView(current) }
        }
        root = null
        ring = null
    }
}
