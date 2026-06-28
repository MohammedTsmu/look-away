package com.eyecare.lookaway.service

import com.eyecare.lookaway.data.Settings
import com.eyecare.lookaway.util.TimeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.ceil

enum class Phase { IDLE, WORKING, BREAK }

data class EngineState(
    val phase: Phase = Phase.IDLE,
    val paused: Boolean = false,
    val inQuietHours: Boolean = false,
    val secondsRemaining: Int = 0,
    val totalSeconds: Int = 0,
    val breaksTaken: Int = 0,
) {
    val isRunning: Boolean get() = phase != Phase.IDLE
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else 1f - secondsRemaining.toFloat() / totalSeconds
}

/**
 * The countdown brain. A single long-lived coroutine alternates between a
 * WORKING phase (counting down the work interval) and a BREAK phase. It is a
 * process-wide singleton so the UI, the service, and the break screen all
 * observe the exact same [state]. The host (the foreground service) wires up
 * the [onShowBreak]/[onBreakEnd] callbacks and keeps [settings] fresh.
 */
object ReminderEngine {

    @Volatile
    var settings: Settings = Settings()

    /** Whether the device screen is currently on. Updated by the service. */
    @Volatile
    private var screenOn: Boolean = true

    /** Invoked once at the start of every break, on the engine's scope. */
    var onShowBreak: (() -> Unit)? = null

    /** Invoked when a break ends (naturally, skipped, or finished early). */
    var onBreakEnd: (() -> Unit)? = null

    /** Invoked every second so the host can refresh its notification. */
    var onTick: ((EngineState) -> Unit)? = null

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private var scope: CoroutineScope? = null
    private var loopJob: Job? = null

    // One-shot control flags consumed by the running loop.
    @Volatile private var skipRequested = false
    @Volatile private var triggerBreakNow = false

    fun start(initial: Settings) {
        settings = initial
        if (loopJob?.isActive == true) return
        val s = CoroutineScope(SupervisorJob())
        scope = s
        _state.value = EngineState(phase = Phase.WORKING, breaksTaken = _state.value.breaksTaken)
        loopJob = s.launch { runLoop() }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        loopJob = null
        skipRequested = false
        triggerBreakNow = false
        onBreakEnd?.invoke()
        _state.value = EngineState(phase = Phase.IDLE, breaksTaken = _state.value.breaksTaken)
    }

    /**
     * Tracks the screen state. When the screen turns off we abandon any active
     * break (nobody is looking) — the working countdown will then hold until the
     * screen comes back on, so reminders only count actual screen-on time.
     */
    fun setScreenOn(on: Boolean) {
        if (screenOn == on) return
        screenOn = on
        if (!on && settings.pauseWhenScreenOff && _state.value.phase == Phase.BREAK) {
            skipRequested = true
        }
        onTick?.invoke(_state.value)
    }

    fun togglePause() = setPaused(!_state.value.paused)

    fun setPaused(paused: Boolean) {
        if (!_state.value.isRunning) return
        _state.value = _state.value.copy(paused = paused)
        onTick?.invoke(_state.value)
    }

    /** End the current break early. [counts] decides whether it tallies as taken. */
    fun endBreak() { if (_state.value.phase == Phase.BREAK) skipRequested = true }

    /** Jump straight to a break from the working phase (used by "preview"). */
    fun breakNow() { if (_state.value.phase == Phase.WORKING) triggerBreakNow = true }

    private suspend fun runLoop() {
        while (scope?.isActive == true) {
            // ---- WORKING ----
            val work = settings.workSeconds
            val result = countdown(Phase.WORKING, work)
            if (result == CONTROL_STOP) break

            // ---- BREAK ----
            onShowBreak?.invoke()
            val breakResult = countdown(Phase.BREAK, settings.breakSeconds)
            onBreakEnd?.invoke()
            if (breakResult != CONTROL_STOP) {
                _state.value = _state.value.copy(breaksTaken = _state.value.breaksTaken + 1)
            }
        }
    }

    /**
     * Counts [total] seconds down in 100 ms ticks. Honors pause and (for the
     * working phase) quiet hours by holding the clock without advancing.
     * Returns one of the CONTROL_* outcomes.
     */
    private suspend fun countdown(phase: Phase, total: Int): Int {
        var remainingMs = total * 1000L
        publish(phase, total, total)
        var lastWhole = total
        skipRequested = false
        triggerBreakNow = false

        while (remainingMs > 0) {
            if (scope?.isActive != true) return CONTROL_STOP
            delay(TICK_MS)

            val st = _state.value
            if (phase == Phase.BREAK && skipRequested) return CONTROL_SKIP
            if (phase == Phase.WORKING && triggerBreakNow) return CONTROL_DONE

            val quiet = phase == Phase.WORKING && isInQuietHours()
            if (st.inQuietHours != quiet) {
                _state.value = st.copy(inQuietHours = quiet)
                onTick?.invoke(_state.value)
            }
            // While the screen is off the user isn't straining their eyes, so
            // freeze the work countdown (and thus never fire a break) until it
            // comes back on.
            val screenHold = phase == Phase.WORKING && settings.pauseWhenScreenOff && !screenOn
            if (st.paused || quiet || screenHold) continue

            remainingMs -= TICK_MS
            val whole = ceil(remainingMs / 1000.0).toInt()
            if (whole != lastWhole) {
                lastWhole = whole
                publish(phase, whole.coerceAtLeast(0), total)
                onTick?.invoke(_state.value)
            }
        }
        return CONTROL_DONE
    }

    private fun publish(phase: Phase, remaining: Int, total: Int) {
        val cur = _state.value
        _state.value = cur.copy(
            phase = phase,
            secondsRemaining = remaining,
            totalSeconds = total,
        )
    }

    private fun isInQuietHours(): Boolean {
        if (!settings.quietHoursEnabled) return false
        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return TimeWindow.contains(minutes, settings.quietStartMinutes, settings.quietEndMinutes)
    }

    private const val TICK_MS = 100L
    private const val CONTROL_DONE = 0
    private const val CONTROL_SKIP = 1
    private const val CONTROL_STOP = 2
}
