package com.xeno.gfx.core.gfx

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.xeno.gfx.core.permission.PermissionHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

data class ApplyResult(
    val isSuccess: Boolean,
    val message: String,
    val iniWritten: Boolean = false,
    val activeSavWritten: Boolean = false
)

/**
 * Storage manager that bridges Android 11+ (API 30-35) Scoped Storage (Storage Access Framework)
 * and legacy direct filesystem access for BGMI / PUBG Mobile engine configurations.
 */
object GfxFileManager {

    private const val RELATIVE_SAVED_DIR = "UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved"
    private const val CONFIG_SUBDIR = "Config/Android"
    private const val SAVEGAMES_SUBDIR = "SaveGames"
    private const val USER_CUSTOM_INI = "UserCustom.ini"
    private const val ACTIVE_SAV = "Active.sav"

    /**
     * Builds an [Intent] requesting Storage Access Framework (SAF) folder access for Android 11+.
     * Targets `Android/data/<package_name>/files` or `Android/data`.
     */
    fun createScopedStorageIntent(packageName: String = "com.pubg.imobile"): Intent {
        val targetPath = "Android/data/$packageName/files"
        val authority = "com.android.externalstorage.documents"
        val treeUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:$targetPath")

        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeUri)
            }
        }
    }

    /**
     * Checks whether the app has required storage permission (either SAF tree grant or legacy).
     */
    fun hasAccess(context: Context, treeUri: Uri?): Boolean {
        return PermissionHelper.hasStorageAccess(context, treeUri)
    }

    /**
     * Tests whether direct filesystem write is permitted without SAF.
     */
    fun canWriteDirectly(context: Context, targetPackage: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return true
        }
        val testDir = File(
            Environment.getExternalStorageDirectory(),
            "Android/data/$targetPackage/files/$RELATIVE_SAVED_DIR/$CONFIG_SUBDIR"
        )
        return try {
            if (testDir.exists()) testDir.canWrite() else testDir.mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Applies both `UserCustom.ini` and `Active.sav` to the target game directory.
     */
    fun applyGfxConfig(
        context: Context,
        config: GfxConfig,
        treeUri: Uri? = null
    ): ApplyResult {
        return try {
            val iniPayload = Ue4CvarCodec.generateUserCustomIni(config)

            // 1. Try Direct File if Android < 30 with permission, or if Environment.isExternalStorageManager()
            if (canWriteDirectly(context, config.targetPackage)) {
                return applyViaDirectFile(context, config, iniPayload)
            }

            // 2. Try SAF (Storage Access Framework) on Android 11+ (API 30+) or if treeUri is provided
            if (treeUri != null) {
                return applyViaSaf(context, config, iniPayload, treeUri)
            }

            // 3. Informative error message based on Android version
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                ApplyResult(
                    isSuccess = false,
                    message = "Storage permission required. Please grant storage access in Settings."
                )
            } else {
                ApplyResult(
                    isSuccess = false,
                    message = "Storage Access Framework permission required for Android 11+. Please link your game folder."
                )
            }
        } catch (e: Exception) {
            ApplyResult(
                isSuccess = false,
                message = "Failed to apply GFX configuration: ${e.message}"
            )
        }
    }

    private fun applyViaDirectFile(
        context: Context,
        config: GfxConfig,
        iniPayload: String
    ): ApplyResult {
        val baseDir = File(
            Environment.getExternalStorageDirectory(),
            "Android/data/${config.targetPackage}/files/$RELATIVE_SAVED_DIR"
        )

        val configDir = File(baseDir, CONFIG_SUBDIR)
        if (!configDir.exists() && !configDir.mkdirs()) {
            return ApplyResult(
                isSuccess = false,
                message = "Could not create config directory in ${baseDir.path}. Check storage permissions."
            )
        }

        val saveGamesDir = File(baseDir, SAVEGAMES_SUBDIR)
        if (!saveGamesDir.exists()) {
            saveGamesDir.mkdirs()
        }

        // 1. Write UserCustom.ini
        val iniFile = File(configDir, USER_CUSTOM_INI)
        FileOutputStream(iniFile, false).use {
            it.write(iniPayload.toByteArray(Charsets.ISO_8859_1))
        }

        // 2. Patch & Write Active.sav
        val savFile = File(saveGamesDir, ACTIVE_SAV)
        val sourceBytes = if (savFile.exists()) {
            savFile.readBytes()
        } else {
            ActiveSavPatcher.createFromTemplate(context, config)
        }
        val patchedBytes = ActiveSavPatcher.patchActiveSav(sourceBytes, config)
        FileOutputStream(savFile, false).use {
            it.write(patchedBytes)
        }

        return ApplyResult(
            isSuccess = true,
            message = "Config applied successfully (${config.presetName})",
            iniWritten = true,
            activeSavWritten = true
        )
    }

    private fun applyViaSaf(
        context: Context,
        config: GfxConfig,
        iniPayload: String,
        treeUri: Uri?
    ): ApplyResult {
        if (treeUri == null) {
            return ApplyResult(
                isSuccess = false,
                message = "Storage Access Framework permission required for Android 11+"
            )
        }

        // Attempt 1: DocumentFile tree traversal
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null) {
                val filesDoc = resolveFilesDirectory(rootDoc, config.targetPackage)
                if (filesDoc != null) {
                    val ue4Doc = getOrCreateDir(filesDoc, "UE4Game")
                    val ste1Doc = getOrCreateDir(ue4Doc, "ShadowTrackerExtra")
                    val ste2Doc = getOrCreateDir(ste1Doc, "ShadowTrackerExtra")
                    val savedDoc = getOrCreateDir(ste2Doc, "Saved")

                    // 1. Config -> Android -> UserCustom.ini
                    val configDoc = getOrCreateDir(savedDoc, "Config")
                    val androidDoc = getOrCreateDir(configDoc, "Android")
                    val existingIni = androidDoc.findFile(USER_CUSTOM_INI)
                    existingIni?.delete()
                    val iniDoc = androidDoc.createFile("text/plain", USER_CUSTOM_INI)

                    if (iniDoc != null) {
                        context.contentResolver.openOutputStream(iniDoc.uri, "wt")?.use { os ->
                            os.write(iniPayload.toByteArray(Charsets.ISO_8859_1))
                        }

                        // 2. SaveGames -> Active.sav
                        val saveGamesDoc = getOrCreateDir(savedDoc, SAVEGAMES_SUBDIR)
                        val existingSav = saveGamesDoc.findFile(ACTIVE_SAV)
                        val sourceBytes = if (existingSav != null) {
                            context.contentResolver.openInputStream(existingSav.uri)?.use { it.readBytes() }
                                ?: ActiveSavPatcher.createFromTemplate(context, config)
                        } else {
                            ActiveSavPatcher.createFromTemplate(context, config)
                        }

                        val patchedBytes = ActiveSavPatcher.patchActiveSav(sourceBytes, config)
                        existingSav?.delete()
                        val savDoc = saveGamesDoc.createFile("application/octet-stream", ACTIVE_SAV)
                        if (savDoc != null) {
                            context.contentResolver.openOutputStream(savDoc.uri, "wt")?.use { os ->
                                os.write(patchedBytes)
                            }
                        }

                        return ApplyResult(
                            isSuccess = true,
                            message = "Config applied successfully via SAF (${config.presetName})",
                            iniWritten = true,
                            activeSavWritten = savDoc != null
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Traversal attempt encountered an error, proceed to direct Document URI method
        }

        // Attempt 2: Direct Document URI resolution (GFX Tool proven method)
        return try {
            val relativeIniPath = "Android/data/${config.targetPackage}/files/$RELATIVE_SAVED_DIR/$CONFIG_SUBDIR/$USER_CUSTOM_INI"
            val encodedIniPath = relativeIniPath.replace("/", "%2F")
            val directIniUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$encodedIniPath")

            context.contentResolver.openOutputStream(directIniUri, "rwt")?.use { os ->
                os.write(iniPayload.toByteArray(Charsets.ISO_8859_1))
            }

            // Also attempt Active.sav
            val relativeSavPath = "Android/data/${config.targetPackage}/files/$RELATIVE_SAVED_DIR/$SAVEGAMES_SUBDIR/$ACTIVE_SAV"
            val encodedSavPath = relativeSavPath.replace("/", "%2F")
            val directSavUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$encodedSavPath")

            val sourceBytes = try {
                context.contentResolver.openInputStream(directSavUri)?.use { it.readBytes() }
                    ?: ActiveSavPatcher.createFromTemplate(context, config)
            } catch (e: Exception) {
                ActiveSavPatcher.createFromTemplate(context, config)
            }
            val patchedBytes = ActiveSavPatcher.patchActiveSav(sourceBytes, config)
            context.contentResolver.openOutputStream(directSavUri, "rwt")?.use { os ->
                os.write(patchedBytes)
            }

            ApplyResult(
                isSuccess = true,
                message = "Config applied successfully via direct SAF (${config.presetName})",
                iniWritten = true,
                activeSavWritten = true
            )
        } catch (e: Exception) {
            ApplyResult(
                isSuccess = false,
                message = "Failed to write via SAF: ${e.message}. Please re-link your game folder."
            )
        }
    }

    private fun resolveFilesDirectory(root: DocumentFile, targetPackage: String): DocumentFile? {
        val rootName = root.name ?: ""
        if (rootName.equals("files", ignoreCase = true) || root.findFile("UE4Game") != null) {
            return root
        }
        val targetPkgDoc = if (rootName.equals(targetPackage, ignoreCase = true)) {
            root
        } else {
            root.findFile(targetPackage)
        }
        if (targetPkgDoc != null) {
            return targetPkgDoc.findFile("files") ?: targetPkgDoc.createDirectory("files")
        }
        // If user selected 'Android' or root, navigate down Android -> data -> <targetPackage> -> files
        val dataDoc = if (rootName.equals("Android", ignoreCase = true)) {
            root.findFile("data")
        } else {
            root.findFile("Android")?.findFile("data") ?: root.findFile("data")
        }
        if (dataDoc != null) {
            val pkg = dataDoc.findFile(targetPackage) ?: dataDoc.createDirectory(targetPackage)
            return pkg?.let { it.findFile("files") ?: it.createDirectory("files") }
        }
        return root
    }

    private fun getOrCreateDir(parent: DocumentFile, dirName: String): DocumentFile {
        return parent.findFile(dirName) ?: parent.createDirectory(dirName) ?: parent
    }
}
