package com.xeno.gfx.core.gfx

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance binary patcher for Unreal Engine 4 `Active.sav` save files (GVAS format).
 * Directly reads and patches `BattleFPS`, `FPSLevel`, `LobbyFPS`, `BattleRenderQuality`,
 * and `BattleRenderStyle` in the binary stream without corrupting file structure.
 */
object ActiveSavPatcher {

    private const val INT_PROPERTY_HEADER = "\u000c\u0000\u0000\u0000IntProperty\u0000\u0004\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"

    /**
     * Patches an existing `Active.sav` byte array with the desired [GfxConfig] values.
     * Returns a new [ByteArray] with patched properties, preserving overall binary integrity.
     */
    fun patchActiveSav(sourceBytes: ByteArray, config: GfxConfig): ByteArray {
        val patched = sourceBytes.clone()

        // Patch FPS parameters
        patchIntProperty(patched, "BattleFPS", config.fpsTarget.levelId)
        patchIntProperty(patched, "FPSLevel", config.fpsTarget.levelId)
        patchIntProperty(patched, "LobbyFPS", config.fpsTarget.levelId)

        // Patch Graphics Quality
        patchIntProperty(patched, "BattleRenderQuality", config.graphicQuality.id)
        patchIntProperty(patched, "LobbyRenderQuality", config.graphicQuality.id)

        // Patch Render Style
        patchIntProperty(patched, "BattleRenderStyle", config.style.id)
        patchIntProperty(patched, "LobbyRenderStyle", config.style.id)

        return patched
    }

    /**
     * Reads an IntProperty value from an `Active.sav` byte stream.
     */
    fun readIntProperty(bytes: ByteArray, propertyName: String): Int? {
        val targetPattern = buildPattern(propertyName)
        val index = indexOfPattern(bytes, targetPattern)
        if (index == -1) return null

        val valOffset = index + targetPattern.size
        if (valOffset + 4 > bytes.size) return null

        return ByteBuffer.wrap(bytes, valOffset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }

    /**
     * Patches an IntProperty in-place if found. Returns true if modified.
     */
    fun patchIntProperty(bytes: ByteArray, propertyName: String, newValue: Int): Boolean {
        val targetPattern = buildPattern(propertyName)
        val index = indexOfPattern(bytes, targetPattern)
        if (index == -1) return false

        val valOffset = index + targetPattern.size
        if (valOffset + 4 > bytes.size) return false

        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(newValue)
        System.arraycopy(buffer.array(), 0, bytes, valOffset, 4)
        return true
    }

    /**
     * Loads the base `Active.sav` template from app assets and applies [config].
     */
    fun createFromTemplate(context: Context, config: GfxConfig): ByteArray {
        val inputStream: InputStream = context.assets.open("Active.sav")
        val templateBytes = inputStream.use { it.readBytes() }
        return patchActiveSav(templateBytes, config)
    }

    private fun buildPattern(propertyName: String): ByteArray {
        val nameBytes = (propertyName + "\u0000").toByteArray(Charsets.ISO_8859_1)
        val headerBytes = INT_PROPERTY_HEADER.toByteArray(Charsets.ISO_8859_1)
        val pattern = ByteArray(nameBytes.size + headerBytes.size)
        System.arraycopy(nameBytes, 0, pattern, 0, nameBytes.size)
        System.arraycopy(headerBytes, 0, pattern, nameBytes.size, headerBytes.size)
        return pattern
    }

    private fun indexOfPattern(source: ByteArray, pattern: ByteArray): Int {
        if (pattern.isEmpty() || source.size < pattern.size) return -1
        outer@ for (i in 0..source.size - pattern.size) {
            for (j in pattern.indices) {
                if (source[i + j] != pattern[j]) {
                    continue@outer
                }
            }
            return i
        }
        return -1
    }
}
