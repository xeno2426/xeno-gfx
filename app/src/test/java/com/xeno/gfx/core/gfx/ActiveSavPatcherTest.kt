package com.xeno.gfx.core.gfx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ActiveSavPatcherTest {

    private fun createSyntheticGvasBytes(
        battleFps: Int = 1,
        fpsLevel: Int = 1,
        battleQuality: Int = 1,
        battleStyle: Int = 1
    ): ByteArray {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("GVAS".toByteArray(Charsets.ISO_8859_1))

        fun putIntProp(name: String, value: Int) {
            buffer.put(name.toByteArray(Charsets.ISO_8859_1))
            buffer.put(0.toByte()) // null term
            buffer.putInt(12) // strlen of "IntProperty\0"
            buffer.put("IntProperty\u0000".toByteArray(Charsets.ISO_8859_1))
            buffer.putInt(4) // size 4
            buffer.putInt(0) // array index
            buffer.put(0.toByte()) // tag terminator
            buffer.putInt(value) // little endian int32 value
        }

        putIntProp("BattleFPS", battleFps)
        putIntProp("FPSLevel", fpsLevel)
        putIntProp("LobbyFPS", battleFps)
        putIntProp("BattleRenderQuality", battleQuality)
        putIntProp("LobbyRenderQuality", battleQuality)
        putIntProp("BattleRenderStyle", battleStyle)
        putIntProp("LobbyRenderStyle", battleStyle)

        val result = ByteArray(buffer.position())
        System.arraycopy(buffer.array(), 0, result, 0, result.size)
        return result
    }

    @Test
    fun `test reading IntProperty from GVAS binary format`() {
        val bytes = createSyntheticGvasBytes(battleFps = 3, fpsLevel = 3, battleQuality = 1, battleStyle = 2)

        val fps = ActiveSavPatcher.readIntProperty(bytes, "BattleFPS")
        val quality = ActiveSavPatcher.readIntProperty(bytes, "BattleRenderQuality")
        val style = ActiveSavPatcher.readIntProperty(bytes, "BattleRenderStyle")

        assertEquals(3, fps)
        assertEquals(1, quality)
        assertEquals(2, style)
    }

    @Test
    fun `test patching ActiveSav preserves buffer size and updates values`() {
        val original = createSyntheticGvasBytes(battleFps = 1, fpsLevel = 1, battleQuality = 1, battleStyle = 1)
        val config = GfxConfig.POTATO_ZERO_LAG_30.copy(
            fpsTarget = FpsTarget.HIGH_30,
            graphicQuality = GraphicQuality.SMOOTH,
            style = RenderStyle.CLASSIC
        )

        val patched = ActiveSavPatcher.patchActiveSav(original, config)

        assertEquals("Patched buffer size must remain identical to preserve GVAS layout", original.size, patched.size)
        assertEquals(3, ActiveSavPatcher.readIntProperty(patched, "BattleFPS"))
        assertEquals(3, ActiveSavPatcher.readIntProperty(patched, "FPSLevel"))
        assertEquals(3, ActiveSavPatcher.readIntProperty(patched, "LobbyFPS"))
        assertEquals(1, ActiveSavPatcher.readIntProperty(patched, "BattleRenderQuality"))
        assertEquals(1, ActiveSavPatcher.readIntProperty(patched, "BattleRenderStyle"))
    }

    @Test
    fun `test patching real ActiveSav asset template`() {
        val assetFile = File("/workspaces/xeno-gfx/app/src/main/assets/Active.sav")
        if (assetFile.exists()) {
            val templateBytes = assetFile.readBytes()
            val config = GfxConfig.SMOOTH_60_FPS

            val patched = ActiveSavPatcher.patchActiveSav(templateBytes, config)

            assertEquals(templateBytes.size, patched.size)
            assertEquals(5, ActiveSavPatcher.readIntProperty(patched, "BattleFPS")) // Extreme 60 = 5
            assertEquals(1, ActiveSavPatcher.readIntProperty(patched, "BattleRenderQuality")) // Smooth = 1
        }
    }
}
