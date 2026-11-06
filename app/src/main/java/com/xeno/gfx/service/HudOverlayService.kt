package com.xeno.gfx.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.xeno.gfx.R
import com.xeno.gfx.XenoGfxApp
import com.xeno.gfx.core.model.HudConfig
import com.xeno.gfx.core.repository.TelemetryRepository
import com.xeno.gfx.ui.MainActivity
import com.xeno.gfx.ui.overlay.HudOverlayView
import com.xeno.gfx.ui.theme.XenoGfxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.abs

class HudOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.xeno.gfx.action.START_HUD"
        const val ACTION_STOP = "com.xeno.gfx.action.STOP_HUD"
        private const val NOTIFICATION_ID = 2026

        fun start(context: Context) {
            val intent = Intent(context, HudOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, HudOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var telemetryRepository: TelemetryRepository
    private val lifecycleOwner = ComposeLifecycleOwner()

    private var overlayView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        telemetryRepository = TelemetryRepository.getInstance(this)
        lifecycleOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showOverlay()
                lifecycleOwner.onResume()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        lifecycleOwner.onPause()
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 120
        }
        windowLayoutParams = params

        val composeView = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val metrics by telemetryRepository.metrics.collectAsState()
                XenoGfxTheme {
                    HudOverlayView(
                        metrics = metrics,
                        config = HudConfig(),
                        onCloseClick = { stopSelf() }
                    )
                }
            }
        }

        // Draggable Touch Handler
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        composeView.setOnTouchListener { _, event ->
            val p = windowLayoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchX).toInt()
                    val deltaY = (event.rawY - touchY).toInt()

                    p.x = initialX + deltaX
                    p.y = initialY + deltaY
                    windowManager.updateViewLayout(composeView, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val movedDistance = abs(event.rawX - touchX) + abs(event.rawY - touchY)
                    if (movedDistance < 10) {
                        composeView.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }

    private fun buildNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, HudOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, XenoGfxApp.HUD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.hud_notification_title))
            .setContentText(getString(R.string.hud_notification_text))
            .setContentIntent(pendingAppIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.btn_stop_hud), pendingStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
