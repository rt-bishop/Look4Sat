package com.rtbishop.lookingsat.api

import com.rtbishop.lookingsat.repo.Transmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TLE_AMATEUR = "https://celestrak.com/NORAD/elements/amateur.txt"
private const val TLE_WEATHER = "https://celestrak.com/NORAD/elements/weather.txt"

class RemoteDataSource(private val api: TransmittersApi, private val client: OkHttpClient) {

    private val requestAmateur = Request.Builder().url(TLE_AMATEUR).build()
    private val requestWeather = Request.Builder().url(TLE_WEATHER).build()

    suspend fun fetchTles(): ByteArray {
        var streamAmateur: ByteArray? = null
        var streamWeather: ByteArray? = null
        withContext(Dispatchers.IO) {
            streamAmateur = client.newCall(requestAmateur).execute().body()?.bytes()
            streamWeather = client.newCall(requestWeather).execute().body()?.bytes()
        }
        return streamAmateur ?: ByteArray(0).plus(streamWeather ?: ByteArray(0))
    }

    suspend fun fetchTransmitters(): List<Transmitter> {
        return api.fetchTransmittersList()
    }
}