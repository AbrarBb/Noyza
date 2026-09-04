package com.khatibstudio.noyza.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*

/**
 * Adaptive banner ad slot.
 * Shows nothing for Premium / Ads Removed users.
 *
 * Per spec: NEVER place over the live noise measurement.
 * Only use at the bottom of content after all meaningful information.
 */
@Composable
fun AdBannerSlot(
    modifier: Modifier = Modifier,
    isAdsRemoved: Boolean = false
) {
    if (isAdsRemoved) return

    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, 360))
                adUnitId = com.khatibstudio.noyza.BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
