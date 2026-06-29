package com.eyecare.lookaway.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/** A simple circular countdown: a faint full track, a white progress arc, and the seconds in the middle. */
class CountdownRingView(context: Context) : View(context) {

    private var progress = 1f
    private var number = 0

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(46, 255, 255, 255)
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val oval = RectF()

    fun set(number: Int, progress: Float) {
        this.number = number
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        val stroke = size * 0.07f
        track.strokeWidth = stroke
        arc.strokeWidth = stroke
        text.textSize = size * 0.34f

        val inset = stroke / 2f + 1f
        val left = (width - size) / 2f + inset
        val top = (height - size) / 2f + inset
        oval.set(left, top, left + size - 2 * inset, top + size - 2 * inset)

        canvas.drawArc(oval, 0f, 360f, false, track)
        canvas.drawArc(oval, -90f, 360f * progress, false, arc)

        val cy = height / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(number.toString(), width / 2f, cy, text)
    }
}
