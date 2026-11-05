package com.xeno.gfx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.xeno.gfx.ui.dashboard.DashboardScreen
import com.xeno.gfx.ui.dashboard.DashboardViewModel
import com.xeno.gfx.ui.theme.XenoGfxTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            XenoGfxTheme {
                val uiState by viewModel.uiState.collectAsState()
                DashboardScreen(
                    uiState = uiState,
                    onToggleManualHud = { viewModel.toggleManualHud() },
                    onToggleAutoHud = { enabled -> viewModel.toggleAutoHud(enabled) },
                    onToggleTrackedGame = { pkg -> viewModel.toggleTrackedGame(pkg) }
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
