package com.khatibstudio.noyza.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.khatibstudio.noyza.notification.NoyZaNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MeasurementService : Service() {

    @Inject
    lateinit var notificationManager: NoyZaNotificationManager

    companion object {
        const val ACTION_START = "com.khatibstudio.noyza.START_MEASUREMENT"
        const val ACTION_STOP = "com.khatibstudio.noyza.STOP_MEASUREMENT"
        const val EXTRA_ACTIVITY_NAME = "extra_activity_name"

        fun start(context: Context, activityName: String) {
            val intent = Intent(context, MeasurementService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACTIVITY_NAME, activityName)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MeasurementService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val activityName = intent?.getStringExtra(EXTRA_ACTIVITY_NAME) ?: "Focus"
                val notification = notificationManager.buildSessionNotification(
                    activityName = activityName,
                    durationText = "Session running",
                    dbText = "Measuring noise..."
                )
                startForeground(NoyZaNotificationManager.NOTIF_ID_SESSION, notification)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
