package com.eyecare.lookaway

import com.eyecare.lookaway.util.TimeWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeWindowTest {

    @Test
    fun sameDayWindow_insideAndOutside() {
        // 09:00 .. 17:00
        val start = 9 * 60
        val end = 17 * 60
        assertTrue(TimeWindow.contains(10 * 60, start, end))
        assertTrue(TimeWindow.contains(start, start, end)) // inclusive start
        assertFalse(TimeWindow.contains(end, start, end))  // exclusive end
        assertFalse(TimeWindow.contains(8 * 60, start, end))
        assertFalse(TimeWindow.contains(18 * 60, start, end))
    }

    @Test
    fun overnightWindow_wrapsPastMidnight() {
        // 22:00 .. 07:00
        val start = 22 * 60
        val end = 7 * 60
        assertTrue(TimeWindow.contains(23 * 60, start, end))   // late evening
        assertTrue(TimeWindow.contains(2 * 60, start, end))    // after midnight
        assertTrue(TimeWindow.contains(start, start, end))     // inclusive start
        assertFalse(TimeWindow.contains(end, start, end))      // exclusive end
        assertFalse(TimeWindow.contains(12 * 60, start, end))  // midday
    }

    @Test
    fun emptyWindow_isNeverInside() {
        assertFalse(TimeWindow.contains(0, 600, 600))
        assertFalse(TimeWindow.contains(600, 600, 600))
    }
}
