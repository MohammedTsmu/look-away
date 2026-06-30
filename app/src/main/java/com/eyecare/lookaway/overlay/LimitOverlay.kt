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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import com.eyecare.lookaway.R
import com.eyecare.lookaway.util.LocaleManager

/**
 * A calming full-screen reminder shown while the user is inside an app that has
 * passed its daily limit. Not a block — just a strong, dismissible nudge with
 * "Snooze 5 min" and "Dismiss" actions. Drawn as an overlay window so it works
 * across OEMs (needs "Display over other apps").
 */
object LimitOverlay {

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
        onSnooze: () -> Unit,
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
            setPadding(px(32), px(32), px(32), px(32))
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

        column.addView(text(l.getString(R.string.app_limit_nudge_title), 16f, false, 210))
        column.addView(text(appLabel, 30f, true).apply { setPadding(0, px(8), 0, px(4)) })
        column.addView(text(usedText, 16f, false, 210).apply { setPadding(0, 0, 0, px(28)) })

        fun button(label: String, filled: Boolean, onClick: () -> Unit) = TextView(l).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = px(28).toFloat()
                setColor(if (filled) Color.argb(60, 255, 255, 255) else Color.argb(28, 255, 255, 255))
            }
            setPadding(px(36), px(12), px(36), px(12))
            isClickable = true
            setOnClickListener { onClick() }
        }

        column.addView(
            button(l.getString(R.string.limit_snooze), true) { onSnooze() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = px(12) },
        )
        column.addView(
            button(l.getString(R.string.limit_dismiss), false) { onDismiss() },
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
