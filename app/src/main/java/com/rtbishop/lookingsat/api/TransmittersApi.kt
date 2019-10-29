package com.rtbishop.lookingsat.api

import com.rtbishop.lookingsat.repo.Transmitter
import retrofit2.http.GET

interface TransmittersApi {

    @GET("transmitters/")
    suspend fun fetchTransmittersList(): List<Transmitter>
}