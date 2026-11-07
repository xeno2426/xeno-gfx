package com.xeno.gfx.core.gfx

/**
 * Graphics quality levels supported by BGMI / PUBG Mobile (UE4 ShadowTrackerExtra).
 */
enum class GraphicQuality(val id: Int, val displayName: String, val cvarValue: Int) {
    SMOOTH(1, "Smooth", 0),
    BALANCED(2, "Balanced", 1),
    HD(3, "HD", 2),
    HDR(4, "HDR", 3),
    ULTRA_HD(5, "Ultra HD", 4)
}

/**
 * FPS target levels in UE4 BattleFPS / FPSLevel property.
 */
enum class FpsTarget(val levelId: Int, val targetFps: Int, val displayName: String) {
    LOW_15(1, 15, "Low (15 FPS)"),
    MEDIUM_20(2, 20, "Medium (20 FPS)"),
    HIGH_30(3, 30, "High (30 FPS)"),
    ULTRA_40(4, 40, "Ultra (40 FPS)"),
    EXTREME_60(5, 60, "Extreme (60 FPS)"),
    FPS_90(6, 90, "90 FPS"),
    FPS_120(7, 120, "120 FPS")
}

/**
 * Resolution scale factor to reduce GPU rendering load on low-RAM/budget GPUs.
 */
enum class ResolutionScale(val scale: Float, val displayName: String, val lodScale: Float) {
    POTATO_540P(0.55f, "Potato (540p)", 0.6f),
    LOW_720P(0.70f, "Low (720p)", 0.75f),
    BALANCED_800P(0.80f, "Balanced (800p)", 0.85f),
    HD_1080P(1.0f, "Default (1080p)", 1.0f)
}

/**
 * In-game rendering color styles.
 */
enum class RenderStyle(val id: Int, val displayName: String) {
    CLASSIC(1, "Classic"),
    COLORFUL(2, "Colorful"),
    REALISTIC(3, "Realistic"),
    SOFT(4, "Soft"),
    MOVIE(5, "Movie")
}

/**
 * Graphics rendering API backend.
 */
enum class GraphicsApi(val cvarValue: Int, val displayName: String) {
    OPENGL_ES(0, "OpenGL ES (Recommended for 4GB)"),
    VULKAN(1, "Vulkan (Android 10+)")
}

/**
 * Audio / Sound quality tier.
 */
enum class SoundQuality(val cvarValue: Int, val displayName: String) {
    LOW(0, "Low (Saves CPU)"),
    HIGH(1, "High"),
    ULTRA(2, "Ultra")
}

/**
 * Comprehensive Graphics Configuration model for BGMI / PUBG Mobile.
 */
data class GfxConfig(
    val presetName: String = "Balanced 30 FPS",
    val graphicQuality: GraphicQuality = GraphicQuality.SMOOTH,
    val fpsTarget: FpsTarget = FpsTarget.HIGH_30,
    val resolutionScale: ResolutionScale = ResolutionScale.BALANCED_800P,
    val shadowsEnabled: Boolean = false,
    val antiAliasingEnabled: Boolean = false,
    val graphicsApi: GraphicsApi = GraphicsApi.OPENGL_ES,
    val soundQuality: SoundQuality = SoundQuality.LOW,
    val style: RenderStyle = RenderStyle.COLORFUL,
    val targetPackage: String = "com.pubg.imobile"
) {
    companion object {
        /**
         * Ultra-low overhead preset tailored for 3-4GB RAM budget devices.
         * Guarantees rock-solid 30 FPS with zero stutters.
         */
        val POTATO_ZERO_LAG_30 = GfxConfig(
            presetName = "Potato Zero-Lag 30 FPS",
            graphicQuality = GraphicQuality.SMOOTH,
            fpsTarget = FpsTarget.HIGH_30,
            resolutionScale = ResolutionScale.POTATO_540P,
            shadowsEnabled = false,
            antiAliasingEnabled = false,
            graphicsApi = GraphicsApi.OPENGL_ES,
            soundQuality = SoundQuality.LOW,
            style = RenderStyle.CLASSIC
        )

        /**
         * Optimized 60 FPS preset for smooth competitive gameplay.
         */
        val SMOOTH_60_FPS = GfxConfig(
            presetName = "Smooth 60 FPS",
            graphicQuality = GraphicQuality.SMOOTH,
            fpsTarget = FpsTarget.EXTREME_60,
            resolutionScale = ResolutionScale.LOW_720P,
            shadowsEnabled = false,
            antiAliasingEnabled = false,
            graphicsApi = GraphicsApi.OPENGL_ES,
            soundQuality = SoundQuality.HIGH,
            style = RenderStyle.COLORFUL
        )

        /**
         * Balanced 40 FPS preset balancing visual fidelity with battery & thermal stability.
         */
        val BALANCED_40_FPS = GfxConfig(
            presetName = "Balanced 40 FPS",
            graphicQuality = GraphicQuality.BALANCED,
            fpsTarget = FpsTarget.ULTRA_40,
            resolutionScale = ResolutionScale.BALANCED_800P,
            shadowsEnabled = false,
            antiAliasingEnabled = false,
            graphicsApi = GraphicsApi.OPENGL_ES,
            soundQuality = SoundQuality.HIGH,
            style = RenderStyle.COLORFUL
        )

        /**
         * Maximum refresh rate preset for devices with 90Hz+ displays.
         */
        val COMPETITIVE_90_FPS = GfxConfig(
            presetName = "Competitive 90 FPS",
            graphicQuality = GraphicQuality.SMOOTH,
            fpsTarget = FpsTarget.FPS_90,
            resolutionScale = ResolutionScale.LOW_720P,
            shadowsEnabled = false,
            antiAliasingEnabled = false,
            graphicsApi = GraphicsApi.VULKAN,
            soundQuality = SoundQuality.HIGH,
            style = RenderStyle.COLORFUL
        )
    }
}
