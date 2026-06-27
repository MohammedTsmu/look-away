package com.eyecare.lookaway.util

/** Pure, testable time-of-day window math (minutes since midnight, 0..1439). */
object TimeWindow {

    /**
     * True if [nowMinutes] falls inside the window [startMinutes, endMinutes).
     * Handles windows that wrap past midnight (e.g. 22:00 → 07:00). A window
     * where start == end is treated as empty (never inside).
     */
    fun contains(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean =
        if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
}
