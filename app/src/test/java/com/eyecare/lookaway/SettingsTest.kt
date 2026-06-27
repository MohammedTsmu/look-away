package com.eyecare.lookaway

import com.eyecare.lookaway.data.Settings
import com.eyecare.lookaway.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test
    fun defaultsEncodeThe202020Rule() {
        val s = Settings()
        assertEquals(20, s.workMinutes)
        assertEquals(20, s.breakSeconds)
        assertEquals(20 * 60, s.workSeconds)
        assertEquals(ThemeMode.SYSTEM, s.themeMode)
    }

    @Test
    fun workSecondsTracksWorkMinutes() {
        assertEquals(5 * 60, Settings(workMinutes = 5).workSeconds)
        assertEquals(45 * 60, Settings(workMinutes = 45).workSeconds)
    }
}
