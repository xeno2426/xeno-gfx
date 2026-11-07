package com.xeno.gfx.core.gfx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ue4CvarCodecTest {

    @Test
    fun `test CVar XOR 0x79 encoding matches reverse engineered GFX Tool specimen`() {
        val plainText = "r.StaticMeshLODDistanceScale=0.8"
        val expectedHex = "0B572A0D180D101A341C0A1135363D3D100A0D18171A1C2A1A18151C44495741"

        val encoded = Ue4CvarCodec.encodeCvar(plainText)
        assertEquals(expectedHex, encoded)

        val decoded = Ue4CvarCodec.decodeCvar(expectedHex)
        assertEquals(plainText, decoded)
    }

    @Test
    fun `test CVar round-trip encoding and decoding`() {
        val testCvars = listOf(
            "r.PUBGVersion=5",
            "r.UserQualitySetting=2",
            "r.Shadow.DistanceScale=0.5",
            "r.MobileMSAA=1",
            "r.MobileSimpleShader=1",
            "r.UserVulkanSetting=0"
        )

        for (cvar in testCvars) {
            val encoded = Ue4CvarCodec.encodeCvar(cvar)
            val decoded = Ue4CvarCodec.decodeCvar(encoded)
            assertEquals("Round-trip failed for $cvar", cvar, decoded)
        }
    }

    @Test
    fun `test generateUserCustomIni contains required sections and decoded CVars`() {
        val config = GfxConfig.POTATO_ZERO_LAG_30
        val iniContent = Ue4CvarCodec.generateUserCustomIni(config)

        assertTrue(iniContent.contains("[UserCustom DeviceProfile]"))
        assertTrue(iniContent.contains("[SoundQuality]"))
        assertTrue(iniContent.contains("+CVars=SoundQualityType=0"))

        // Extract and decode all +CVars= under [UserCustom DeviceProfile]
        val decodedLines = iniContent.lines()
            .filter { it.startsWith("+CVars=") && !it.contains("SoundQualityType") }
            .map { line -> Ue4CvarCodec.decodeCvar(line.removePrefix("+CVars=")) }

        assertTrue(decodedLines.contains("r.PUBGVersion=5"))
        assertTrue(decodedLines.contains("r.UserQualitySetting=0"))
        assertTrue(decodedLines.contains("r.MobileSimpleShader=1"))
        assertTrue(decodedLines.contains("r.Shadow.DistanceScale=0.0"))
        assertTrue(decodedLines.contains("r.UserMSAASetting=0"))
    }
}
