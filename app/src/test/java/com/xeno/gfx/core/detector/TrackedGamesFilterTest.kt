package com.xeno.gfx.core.detector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedGamesFilterTest {

    @Test
    fun `test tracked games set operations`() {
        val tracked = mutableSetOf("com.miHoYo.GenshinImpact", "com.activision.callofduty.shooter")

        assertTrue(tracked.contains("com.miHoYo.GenshinImpact"))
        assertFalse(tracked.contains("com.google.android.youtube"))

        tracked.add("com.epicgames.portal")
        assertTrue(tracked.contains("com.epicgames.portal"))

        tracked.remove("com.miHoYo.GenshinImpact")
        assertFalse(tracked.contains("com.miHoYo.GenshinImpact"))
    }
}
