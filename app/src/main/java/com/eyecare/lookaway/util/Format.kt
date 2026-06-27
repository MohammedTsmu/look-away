package com.eyecare.lookaway.util

/** Pure formatting helpers (no Android dependencies, so unit-testable). */
object Format {

    /** Formats a duration in seconds as m:ss (e.g. 75 -> "1:15"). */
    fun clock(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val m = safe / 60
        val s = safe % 60
        return "%d:%02d".format(m, s)
    }
}
