package com.shakeandeat.data

import android.location.Location
import com.shakeandeat.BuildConfig
import java.util.Calendar
import kotlin.math.roundToInt

data class NearbyPlace(
    val placeId: String,
    val name: String,
    val address: String?,
    val distanceMi: Double,
)

data class PlaceHours(
    val openNow: Boolean?,
    val today: String?,
)

class PlacesRepository(
    private val api: PlacesApi = PlacesNetwork.api,
    private val apiKey: String = BuildConfig.PLACES_API_KEY,
) {
    suspend fun nearby(lat: Double, lng: Double, radiusMeters: Double = 1200.0): List<NearbyPlace> {
        check(apiKey.isNotBlank()) { "Missing PLACES_API_KEY" }
        val response = api.searchNearby(
            apiKey = apiKey,
            fieldMask = "places.id,places.displayName,places.formattedAddress,places.location",
            body = NearbyRequest(
                includedTypes = listOf("restaurant", "cafe", "bakery", "meal_takeaway"),
                maxResultCount = 15,
                locationRestriction = LocationRestriction(
                    Circle(center = LatLngDto(lat, lng), radius = radiusMeters),
                ),
            ),
        )
        return response.places.mapNotNull { place ->
            val loc = place.location ?: return@mapNotNull null
            val name = place.displayName?.text?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            NearbyPlace(
                placeId = place.id,
                name = name,
                address = place.formattedAddress,
                distanceMi = milesBetween(lat, lng, loc.latitude, loc.longitude),
            )
        }.sortedBy { it.distanceMi }
    }

    suspend fun hours(placeId: String): PlaceHours {
        check(apiKey.isNotBlank()) { "Missing PLACES_API_KEY" }
        val place = api.getPlace(placeId, apiKey, fieldMask = "id,currentOpeningHours")
        val hours = place.currentOpeningHours
        return PlaceHours(
            openNow = hours?.openNow,
            today = hours?.weekdayDescriptions?.getOrNull(todayIndex())?.substringAfter(": "),
        )
    }

    private fun todayIndex(): Int =
        (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    private fun milesBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val meters = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, meters)
        val miles = meters[0] / 1609.34
        return (miles * 10).roundToInt() / 10.0
    }
}
