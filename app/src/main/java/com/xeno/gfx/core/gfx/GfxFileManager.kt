package com.xeno.gfx.core.gfx

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true // Legacy storage
        }
        if (treeUri == null) return false
        val persisted = context.contentResolver.persistedUriPermissions
        return persisted.any { it.uri == treeUri && it.isWritePermission }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ SAF path
                applyViaSaf(context, config, iniPayload, treeUri)
            } else {
                // Android 8.0 - 10 direct filesystem path
                applyViaDirectFile(context, config, iniPayload)
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
        if (!configDir.exists()) configDir.mkdirs()

        val saveGamesDir = File(baseDir, SAVEGAMES_SUBDIR)
        if (!saveGamesDir.exists()) saveGamesDir.mkdirs()

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

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return ApplyResult(isSuccess = false, message = "Invalid SAF directory reference")

        // Navigate or create: UE4Game -> ShadowTrackerExtra -> ShadowTrackerExtra -> Saved
        val ue4Doc = getOrCreateDir(rootDoc, "UE4Game")
        val ste1Doc = getOrCreateDir(ue4Doc, "ShadowTrackerExtra")
        val ste2Doc = getOrCreateDir(ste1Doc, "ShadowTrackerExtra")
        val savedDoc = getOrCreateDir(ste2Doc, "Saved")

        // 1. Config -> Android -> UserCustom.ini
        val configDoc = getOrCreateDir(savedDoc, "Config")
        val androidDoc = getOrCreateDir(configDoc, "Android")
        val existingIni = androidDoc.findFile(USER_CUSTOM_INI)
        existingIni?.delete()
        val iniDoc = androidDoc.createFile("text/plain", USER_CUSTOM_INI)
            ?: return ApplyResult(isSuccess = false, message = "Could not create UserCustom.ini via SAF")

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
            ?: return ApplyResult(isSuccess = false, message = "Could not create Active.sav via SAF")

        context.contentResolver.openOutputStream(savDoc.uri, "wt")?.use { os ->
            os.write(patchedBytes)
        }

        return ApplyResult(
            isSuccess = true,
            message = "Config applied successfully via SAF (${config.presetName})",
            iniWritten = true,
            activeSavWritten = true
        )
    }

    private fun getOrCreateDir(parent: DocumentFile, dirName: String): DocumentFile {
        return parent.findFile(dirName) ?: parent.createDirectory(dirName) ?: parent
    }
}
