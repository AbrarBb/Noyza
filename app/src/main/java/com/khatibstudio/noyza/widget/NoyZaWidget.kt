package com.khatibstudio.noyza.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.khatibstudio.noyza.MainActivity

/**
 * Premium home screen widget — shows current dB, activity, and suitability.
 * Tapping opens the live measurement screen.
 */
class NoyZaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(16.dp)
                        .padding(16.dp)
                        .clickable(actionStartActivity(launchIntent)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "Noyza",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "-- dB",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 28.sp
                            )
                        )
                        Text(
                            text = "Tap to start measuring",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class NoyZaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoyZaWidget()
}
