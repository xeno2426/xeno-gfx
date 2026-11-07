package com.xeno.gfx.core.permission

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageStatsPermissionIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        // Some OEMs honor the package URI for usage access
        intent.data = Uri.parse("package:${context.packageName}")
        return intent
    }

    /**
     * Checks if the app has required storage permission to write BGMI config/saves.
     * - Android 8.0 - 10: Requires WRITE_EXTERNAL_STORAGE.
     * - Android 11+: Requires Storage Access Framework (SAF tree URI) OR All Files Access.
     */
    fun hasStorageAccess(context: Context, treeUri: Uri? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Android 11+ (API 30+)
        if (Environment.isExternalStorageManager()) {
            return true
        }

        if (treeUri == null) return false
        val persisted = context.contentResolver.persistedUriPermissions
        return persisted.any { it.uri == treeUri && it.isWritePermission }
    }

    fun isAllEssentialGranted(context: Context): Boolean {
        return hasOverlayPermission(context)
    }
}
