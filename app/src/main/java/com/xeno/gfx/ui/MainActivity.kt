package com.xeno.gfx.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.xeno.gfx.core.gfx.GfxFileManager
import com.xeno.gfx.ui.dashboard.DashboardScreen
import com.xeno.gfx.ui.dashboard.DashboardViewModel
import com.xeno.gfx.ui.theme.XenoGfxTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    private val legacyStorageLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    private val safLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.onSafUriGranted(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            XenoGfxTheme {
                val uiState by viewModel.uiState.collectAsState()
                DashboardScreen(
                    uiState = uiState,
                    onToggleManualHud = { viewModel.toggleManualHud() },
                    onToggleAutoHud = { enabled -> viewModel.toggleAutoHud(enabled) },
                    onToggleTrackedGame = { pkg -> viewModel.toggleTrackedGame(pkg) },
                    onManualCleanRam = { viewModel.manualCleanRam() },
                    onLaunchBgmiWithTurbo = { viewModel.launchBgmiWithTurbo() },
                    onToggleContinuousSuppression = { enabled -> viewModel.toggleContinuousSuppression(enabled) },
                    onRequestSafPermission = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            legacyStorageLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val intent = GfxFileManager.createScopedStorageIntent()
                                intent.getParcelableExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI)
                            } else null
                            safLauncher.launch(initialUri)
                        }
                    },
                    onSelectPreset = { preset -> viewModel.selectPreset(preset) },
                    onApplyGfxOnly = { viewModel.applyGfxOnly() },
                    onDismissSessionSummary = { viewModel.dismissSessionSummary() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
