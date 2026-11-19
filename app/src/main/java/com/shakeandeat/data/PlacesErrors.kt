package com.shakeandeat.data

import java.io.IOException
import retrofit2.HttpException

/** Says what actually went wrong with a Places request, so a bad key doesn't look like bad Wi-Fi. */
fun Throwable.toPlacesMessage(): String = when {
    this is IllegalStateException && message?.contains("PLACES_API_KEY") == true ->
        "The Places key is missing. Add PLACES_API_KEY to local.properties and rebuild."
    this is HttpException && (code() == 400 || code() == 401 || code() == 403) ->
        "Google rejected the request (${code()}). Check the Places key, and that Places API (New) is enabled."
    this is HttpException -> "Google returned an error (${code()}). Try again."
    this is IOException -> "No internet connection. Check your network and try again."
    else -> "Couldn't load places. Try again."
}
