package com.shakeandeat.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PlacesApi {

    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body body: NearbyRequest,
    ): NearbyResponse

    @GET("v1/places/{id}")
    suspend fun getPlace(
        @Path("id") placeId: String,
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
    ): PlaceDto
}

@Serializable
data class NearbyRequest(
    val includedTypes: List<String>,
    val maxResultCount: Int,
    val locationRestriction: LocationRestriction,
    val rankPreference: String = "DISTANCE",
)

@Serializable data class LocationRestriction(val circle: Circle)
@Serializable data class Circle(val center: LatLngDto, val radius: Double)
@Serializable data class LatLngDto(val latitude: Double, val longitude: Double)
@Serializable data class NearbyResponse(val places: List<PlaceDto> = emptyList())

@Serializable
data class PlaceDto(
    val id: String = "",
    val displayName: LocalizedText? = null,
    val formattedAddress: String? = null,
    val location: LatLngDto? = null,
    val currentOpeningHours: OpeningHours? = null,
)

@Serializable data class LocalizedText(val text: String = "")

@Serializable
data class OpeningHours(
    val openNow: Boolean? = null,
    val weekdayDescriptions: List<String> = emptyList(),
)

object PlacesNetwork {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val api: PlacesApi = Retrofit.Builder()
        .baseUrl("https://places.googleapis.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PlacesApi::class.java)
}
