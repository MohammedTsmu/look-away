package com.eyecare.lookaway

import com.eyecare.lookaway.util.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", Format.clock(0))
        assertEquals("0:05", Format.clock(5))
        assertEquals("0:59", Format.clock(59))
        assertEquals("1:00", Format.clock(60))
        assertEquals("1:15", Format.clock(75))
        assertEquals("20:00", Format.clock(20 * 60))
    }

    @Test
    fun negativeClampsToZero() {
        assertEquals("0:00", Format.clock(-10))
    }
}
