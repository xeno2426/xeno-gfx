package com.xeno.gfx.core.optimizer

import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager

object AppWhitelist {

    val CRITICAL_SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.providers.telephony"
    )

    val SUPPORTED_BGMI_VARIANTS = setOf(
        "com.pubg.imobile",     // BGMI (Battlegrounds Mobile India)
        "com.tencent.ig",       // PUBG Mobile Global
        "com.tencent.iglite",   // PUBG Mobile Lite
        "com.pubg.krmobile",    // PUBG Korea
        "com.vng.pubgmobile"    // PUBG Vietnam
    )

    fun isProtected(
        packageName: String,
        context: Context,
        targetGamePackage: String = "com.pubg.imobile",
        userWhitelistedPackages: Set<String> = emptySet()
    ): Boolean {
        // 1. Self & Target Game
        if (packageName == context.packageName) return true
        if (packageName == targetGamePackage) return true
        if (SUPPORTED_BGMI_VARIANTS.contains(packageName)) return true

        // 2. Critical System Packages
        if (CRITICAL_SYSTEM_PACKAGES.contains(packageName)) return true
        if (packageName.startsWith("com.android.systemui")) return true

        // 3. User Custom Whitelist
        if (userWhitelistedPackages.contains(packageName)) return true

        // 4. Current Home Launcher
        val launcherPackage = getDefaultLauncherPackage(context)
        if (launcherPackage != null && packageName == launcherPackage) return true

        // 5. Active Keyboard / Input Method
        val defaultImePackage = getDefaultImePackage(context)
        if (defaultImePackage != null && packageName == defaultImePackage) return true

        return false
    }

    fun getDefaultLauncherPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName
    }

    fun getDefaultImePackage(context: Context): String? {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return null
        val enabledImes = imm.enabledInputMethodList
        return enabledImes.firstOrNull()?.packageName
    }
}
