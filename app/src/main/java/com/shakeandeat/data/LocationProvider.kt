package com.shakeandeat.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun hasLocationPermission(context: Context): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

class LocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * The device's current position, or null if none can be found. Each step is time-limited, so
     * the caller never waits forever: fresh high-accuracy fix, then Google's last known position,
     * then the Android system's own last known position (which is what an emulator's
     * "Set Location" feeds).
     */
    @SuppressLint("MissingPermission")
    suspend fun current(): Location? {
        if (!hasLocationPermission(context)) return null

        val fresh = withTimeoutOrNull(8_000) {
            runCatching {
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
            }.getOrNull()
        }
        if (fresh != null) return fresh

        val lastGoogle = withTimeoutOrNull(2_000) {
            runCatching { client.lastLocation.await() }.getOrNull()
        }
        if (lastGoogle != null) return lastGoogle

        return systemLastKnown()
    }

    @SuppressLint("MissingPermission")
    private fun systemLastKnown(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        return listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }
}
