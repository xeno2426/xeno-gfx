package com.xeno.gfx.core.gfx

import java.util.Locale

/**
 * Unreal Engine 4 CVar Codec for BGMI / PUBG Mobile (`UserCustom.ini`).
 * Reverse engineered from GFX Tool (`eu.tsoml.graphicssettings`) and ShadowTrackerExtra engine.
 *
 * All CVars under `[UserCustom DeviceProfile]` are stored as:
 * `+CVars=<HEX_ENCODED_STRING>`
 * where each character byte is XOR-obfuscated with key 0x79 (121).
 */
object Ue4CvarCodec {

    const val XOR_KEY: Byte = 0x79

    /**
     * Obfuscates a plain-text CVar string into the UE4 hex-encoded representation.
     * Example: "r.StaticMeshLODDistanceScale=0.8" -> "0B572A0D180D101A..."
     */
    fun encodeCvar(plainText: String): String {
        val bytes = plainText.toByteArray(Charsets.ISO_8859_1)
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val obfuscated = (b.toInt() xor XOR_KEY.toInt()) and 0xFF
            sb.append(String.format(Locale.US, "%02X", obfuscated))
        }
        return sb.toString()
    }

    /**
     * Decodes a hex-encoded CVar string back into readable plain-text.
     */
    fun decodeCvar(hexString: String): String {
        val cleanHex = hexString.trim()
        val length = cleanHex.length
        if (length % 2 != 0) return ""

        val bytes = ByteArray(length / 2)
        for (i in 0 until length step 2) {
            val byteVal = cleanHex.substring(i, i + 2).toIntOrNull(16) ?: return ""
            bytes[i / 2] = (byteVal xor XOR_KEY.toInt()).toByte()
        }
        return String(bytes, Charsets.ISO_8859_1)
    }

    /**
     * Generates a complete `UserCustom.ini` configuration file payload for a given [GfxConfig].
     */
    fun generateUserCustomIni(config: GfxConfig): String {
        val cvars = mutableListOf<String>()

        // Core Engine Version
        cvars.add("r.PUBGVersion=5")

        // Rendering API Backend (0=OpenGL ES, 1=Vulkan)
        cvars.add("r.UserVulkanSetting=${config.graphicsApi.cvarValue}")

        // Graphics Quality Level & Resolution Scaling
        cvars.add("r.UserQualitySetting=${config.graphicQuality.cvarValue}")
        cvars.add("r.StaticMeshLODDistanceScale=${config.resolutionScale.lodScale}")
        cvars.add("foliage.LODDistanceScale=${if (config.graphicQuality == GraphicQuality.SMOOTH) 0.8f else 1.0f}")
        cvars.add("foliage.MinLOD=${if (config.graphicQuality == GraphicQuality.SMOOTH) 1 else 0}")
        cvars.add("r.ParticleLODBias=${if (config.graphicQuality == GraphicQuality.SMOOTH) 1 else 0}")
        cvars.add("r.EmitterSpawnRateScale=${if (config.graphicQuality == GraphicQuality.SMOOTH) 0.75f else 1.0f}")

        // Material & Shaders
        val isLowEnd = config.graphicQuality == GraphicQuality.SMOOTH
        cvars.add("r.MaterialQualityLevel=${if (isLowEnd) 0 else 1}")
        cvars.add("r.MobileSimpleShader=${if (isLowEnd) 1 else 0}")

        // Shadows Configuration
        if (config.shadowsEnabled) {
            cvars.add("r.Shadow.MaxCSMResolution=1024")
            cvars.add("r.Shadow.CSM.MaxMobileCascades=2")
            cvars.add("r.Shadow.DistanceScale=0.7")
            cvars.add("r.Mobile.DynamicObjectShadow=1")
        } else {
            cvars.add("r.Shadow.MaxCSMResolution=512")
            cvars.add("r.Shadow.CSM.MaxMobileCascades=1")
            cvars.add("r.Shadow.DistanceScale=0.0")
            cvars.add("r.Mobile.DynamicObjectShadow=0")
        }

        // Anti-Aliasing (MSAA)
        if (config.antiAliasingEnabled) {
            cvars.add("r.UserMSAASetting=1")
            cvars.add("r.MobileMSAA=2")
            cvars.add("r.DefaultFeature.AntiAliasing=1")
        } else {
            cvars.add("r.UserMSAASetting=0")
            cvars.add("r.MobileMSAA=1")
            cvars.add("r.DefaultFeature.AntiAliasing=0")
        }

        // Post-processing & Visual Effects (suppressed for peak 30 FPS smoothness)
        cvars.add("r.BloomQuality=0")
        cvars.add("r.LightShaftQuality=0")
        cvars.add("r.DepthOfFieldQuality=0")
        cvars.add("r.RefractionQuality=0")
        cvars.add("r.Mobile.TonemapperFilm=0")
        cvars.add("r.Mobile.AlwaysResolveDepth=0")
        cvars.add("r.Mobile.SceneColorFormat=0")
        cvars.add("r.UserHDRSetting=${if (config.graphicQuality >= GraphicQuality.HDR) 1 else 0}")
        cvars.add("r.MobileHDR=0")

        // Texture Streaming Pool (optimized for 3-4GB RAM footprint)
        cvars.add("r.Streaming.PoolSize=300")
        cvars.add("r.MaxAnisotropy=${if (isLowEnd) 1 else 4}")

        val sb = StringBuilder()
        sb.append("[UserCustom DeviceProfile]\n")
        for (cvar in cvars) {
            sb.append("+CVars=").append(encodeCvar(cvar)).append("\n")
        }

        // Append Sound Quality section
        sb.append("\n[SoundQuality]\n")
        sb.append("+CVars=SoundQualityType=${config.soundQuality.cvarValue}\n")

        return sb.toString()
    }
}
