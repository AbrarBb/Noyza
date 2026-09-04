package com.khatibstudio.noyza.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: NoyZaPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dailyNotifEnabled = preferences.notifDailySummary.first()
                    if (dailyNotifEnabled) {
                        // Reschedule daily summary notification if needed
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
