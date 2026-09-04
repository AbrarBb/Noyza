package com.khatibstudio.noyza.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.*

/**
 * Lightweight, battery-efficient location utility for tagging places and computing proximity.
 * Uses Android framework LocationManager with zero external SDK bloat.
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Check if user has granted coarse or fine location permission.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    /**
     * Get the best available current coordinates (latitude, longitude).
     * Attempts last known location first, then requests a fresh single update with a 4s timeout.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || locationManager == null) return@withContext null

        // 1. Try last known location first for instant response
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }

        // If last known location is fresh (< 2 minutes old), return immediately
        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 120_000L) {
            return@withContext Pair(bestLocation.latitude, bestLocation.longitude)
        }

        // 2. Otherwise request single update with timeout
        val freshLocation = withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> LocationManager.PASSIVE_PROVIDER
                }

                try {
                    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(bestLocation)
                }

                cont.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            }
        }

        val resolved = freshLocation ?: bestLocation
        resolved?.let { Pair(it.latitude, it.longitude) }
    }

    /**
     * Calculate distance between two coordinates in meters using the Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }

    /**
     * Format distance into human-readable label ("350 m", "1.2 km").
     */
    fun formatDistance(meters: Float): String {
        return if (meters < 1000f) {
            "${meters.roundToInt()} m"
        } else {
            val km = meters / 1000f
            "%.1f km".format(km)
        }
    }
}
