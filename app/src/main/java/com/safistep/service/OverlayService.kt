package com.safistep.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.safistep.MainActivity
import com.safistep.R
import com.safistep.SafiStepApp
import com.safistep.data.repository.ReportRepository
import com.safistep.ui.theme.SafiStepTheme
import com.safistep.ui.screens.overlay.BlockingOverlayContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    @Inject lateinit var reportRepo: ReportRepository

    // Lifecycle scaffolding for Compose in a Service
    private val lifecycleRegistry    = LifecycleRegistry(this)
    private val vmStore              = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = vmStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val EXTRA_PLATFORM_NAME     = "platform_name"
        const val EXTRA_PLATFORM_ID       = "platform_id"
        const val EXTRA_PLATFORM_CATEGORY = "platform_category"
        const val EXTRA_MATCHED_KEYWORD   = "matched_keyword"
        const val EXTRA_AMOUNT            = "amount"
        const val EXTRA_PAYBILL           = "paybill"
        const val EXTRA_RAW_TEXT          = "raw_text"
        private const val NOTIF_ID        = 1001
    }

    override fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        startForeground(NOTIF_ID, buildForegroundNotification())

        intent ?: return START_NOT_STICKY

        val platformName     = intent.getStringExtra(EXTRA_PLATFORM_NAME)     ?: "Betting Platform"
        val platformId       = intent.getLongExtra(EXTRA_PLATFORM_ID, -1L)
        val platformCategory = intent.getStringExtra(EXTRA_PLATFORM_CATEGORY) ?: "betting"
        val matchedKeyword   = intent.getStringExtra(EXTRA_MATCHED_KEYWORD)
        val amount           = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0).let { if (it == 0.0) null else it }
        val paybill          = intent.getStringExtra(EXTRA_PAYBILL)
        val rawText          = intent.getStringExtra(EXTRA_RAW_TEXT)

        showOverlay(
            platformName     = platformName,
            platformId       = platformId,
            platformCategory = platformCategory,
            matchedKeyword   = matchedKeyword,
            amount           = amount,
            paybill          = paybill,
            rawText          = rawText
        )

        return START_NOT_STICKY
    }

    private fun showOverlay(
        platformName: String,
        platformId: Long,
        platformCategory: String,
        matchedKeyword: String?,
        amount: Double?,
        paybill: String?,
        rawText: String?
    ) {
        removeOverlay()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setContent {
                SafiStepTheme {
                    BlockingOverlayContent(
                        platformName     = platformName,
                        platformCategory = platformCategory,
                        amount           = amount,
                        paybill          = paybill,
                        onBlock = {
                            scope.launch {
                                reportRepo.recordBlock(
                                    platformId      = platformId.takeIf { it != -1L },
                                    platformName    = platformName,
                                    matchedKeyword  = matchedKeyword,
                                    amountAttempted = amount,
                                    rawStkText      = rawText,
                                    paybillDetected = paybill,
                                    actionTaken     = "blocked"
                                )
                                reportRepo.syncPendingReports()
                            }
                            removeOverlay()
                            stopSelf()
                        },
                        onProceed = {
                            scope.launch {
                                reportRepo.recordBlock(
                                    platformId      = platformId.takeIf { it != -1L },
                                    platformName    = platformName,
                                    matchedKeyword  = matchedKeyword,
                                    amountAttempted = amount,
                                    rawStkText      = rawText,
                                    paybillDetected = paybill,
                                    actionTaken     = "user_overrode"
                                )
                                reportRepo.syncPendingReports()
                            }
                            removeOverlay()
                            stopSelf()
                        }
                    )
                }
            }
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SafiStepApp.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("SafiStep is protecting you")
            .setContentText("A payment to a betting platform was detected")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(intent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        scope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
