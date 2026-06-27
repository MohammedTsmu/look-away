package com.eyecare.lookaway.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.lookaway.R
import com.eyecare.lookaway.service.Feedback
import com.eyecare.lookaway.service.Phase
import com.eyecare.lookaway.service.ReminderEngine
import com.eyecare.lookaway.ui.theme.LookAwayTheme
import com.eyecare.lookaway.ui.theme.accentAt

/**
 * The calming full-screen break. Counts down, then dismisses itself when the
 * engine leaves the BREAK phase (whether it completed, was skipped, or stopped).
 */
class BreakActivity : ComponentActivity() {

    private var finishing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverEverything()
        installBackHandler()

        val settings = ReminderEngine.settings
        if (settings.dimScreen) dimScreen()

        // In full-screen mode this Activity owns the feedback so it lines up
        // with the visuals appearing.
        Feedback.playBreakStart(this, settings.sound, settings.vibrate)

        setContent {
            LookAwayTheme(settings.themeMode, settings.accentIndex) {
                val state by ReminderEngine.state.collectAsStateWithLifecycle()

                // Leave as soon as the break is over.
                if (state.phase != Phase.BREAK && !finishing) {
                    finishing = true
                    Feedback.playBreakEnd(this, settings.sound, settings.vibrate)
                    finishAndRemoveTask()
                }

                BreakContent(
                    secondsRemaining = state.secondsRemaining,
                    totalSeconds = if (state.totalSeconds > 0) state.totalSeconds else settings.breakSeconds,
                    accentIndex = settings.accentIndex,
                    showSkip = !settings.strictMode,
                    onSkip = { ReminderEngine.endBreak() },
                )
            }
        }
    }

    private fun showOverEverything() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dimScreen() {
        window.attributes = window.attributes.apply { screenBrightness = 0.25f }
    }

    private fun installBackHandler() {
        // Don't let Back silently dismiss the break; honor Skip instead.
        onBackPressedDispatcher.addCallback(this) {
            if (!ReminderEngine.settings.strictMode) ReminderEngine.endBreak()
        }
    }
}

@Composable
private fun BreakContent(
    secondsRemaining: Int,
    totalSeconds: Int,
    accentIndex: Int,
    showSkip: Boolean,
    onSkip: () -> Unit,
) {
    val accent = accentAt(accentIndex).seed
    val progress = if (totalSeconds <= 0) 0f else secondsRemaining.toFloat() / totalSeconds
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "ring",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerpToward(accent, Color.Black, 0.45f),
                        Color(0xFF05100F),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.break_headline),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.break_sub),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 12.dp)) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Color.White.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = secondsRemaining.toString(),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (showSkip) {
                Button(
                    onClick = onSkip,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.action_skip))
                }
            }
        }
    }
}

private fun lerpToward(from: Color, to: Color, t: Float): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = 1f,
)
