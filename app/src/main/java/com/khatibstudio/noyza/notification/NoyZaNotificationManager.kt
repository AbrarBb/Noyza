package com.khatibstudio.noyza.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.khatibstudio.noyza.MainActivity
import com.khatibstudio.noyza.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized notification manager.
 * Channels:
 * - noyza_session: Foreground service notification during active session
 * - noyza_alerts: High noise alerts
 * - noyza_summary: Daily environment summaries
 */
@Singleton
class NoyZaNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_SESSION = "noyza_session"
        const val CHANNEL_ALERTS = "noyza_alerts"
        const val CHANNEL_SUMMARY = "noyza_summary"

        const val NOTIF_ID_SESSION = 1001
        const val NOTIF_ID_HIGH_NOISE = 1002
        const val NOTIF_ID_DAILY_SUMMARY = 1003
    }

    private val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sessionChannel = NotificationChannel(
                CHANNEL_SESSION,
                "Active Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while a noise measurement session is running"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Noise Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your environment gets noisy"
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Your daily environment score summary"
            }

            notifManager.createNotificationChannels(listOf(sessionChannel, alertsChannel, summaryChannel))
        }
    }

    fun buildSessionNotification(activityName: String, durationText: String, dbText: String) =
        NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$activityName Session Active")
            .setContentText("$dbText · $durationText")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(buildMainPendingIntent())
            .build()

    fun showHighNoiseAlert(message: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Noise Alert")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(buildMainPendingIntent())
            .build()

        notifManager.notify(NOTIF_ID_HIGH_NOISE, notif)
    }

    fun showDailySummary(score: Int, activityName: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Today's Environment: $score/100")
            .setContentText("Your environments were suitable for $activityName today.")
            .setAutoCancel(true)
            .setContentIntent(buildMainPendingIntent())
            .build()

        notifManager.notify(NOTIF_ID_DAILY_SUMMARY, notif)
    }

    private fun buildMainPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
