package com.xeno.gfx.core.optimizer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppWhitelistTest {

    @Test
    fun `test critical system packages are whitelisted`() {
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("android"))
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("com.android.systemui"))
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("com.google.android.gms"))
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("com.android.phone"))
    }

    @Test
    fun `test BGMI package variants are recognized`() {
        assertTrue(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains("com.pubg.imobile"))
        assertTrue(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains("com.tencent.ig"))
        assertTrue(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains("com.tencent.iglite"))
    }

    @Test
    fun `test non-whitelisted apps can be terminated`() {
        val testApp = "com.instagram.android"
        assertFalse(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains(testApp))
        assertFalse(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains(testApp))
    }
}
