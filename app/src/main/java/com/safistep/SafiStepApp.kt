package com.safistep

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SafiStepApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Foreground service channel (silent)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVICE,
                    "SafiStep Guardian",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Shows SafiStep is actively protecting your payments"
                    setShowBadge(false)
                }
            )

            // Alert channel (blocking detected)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERT,
                    "Payment Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a betting payment is intercepted"
                    enableVibration(true)
                }
            )

            // General channel
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Subscription updates and reminders"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "safistep_service"
        const val CHANNEL_ALERT   = "safistep_alert"
        const val CHANNEL_GENERAL = "safistep_general"
    }
}
