package com.rtbishop.lookingsat.api

import com.rtbishop.lookingsat.repo.Transmitter
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.SequenceInputStream

private const val TLE_AMATEUR = "https://celestrak.com/NORAD/elements/amateur.txt"
private const val TLE_WEATHER = "https://celestrak.com/NORAD/elements/weather.txt"

class RemoteDataSource(private val api: TransmittersApi, private val client: OkHttpClient) {
    private val requestAmateur = Request.Builder().url(TLE_AMATEUR).build()
    private val requestWeather = Request.Builder().url(TLE_WEATHER).build()

    suspend fun fetchTleFiles(): InputStream {
        val streamAmateur = client.newCall(requestAmateur).execute().body()?.byteStream()
        val streamWeather = client.newCall(requestWeather).execute().body()?.byteStream()
        return SequenceInputStream(streamAmateur, streamWeather)
    }

    suspend fun fetchTransmitters(): List<Transmitter> {
        return api.fetchTransmittersList()
    }
}