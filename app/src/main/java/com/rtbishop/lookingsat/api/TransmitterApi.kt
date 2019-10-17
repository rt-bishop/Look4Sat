package com.rtbishop.lookingsat.api

import com.rtbishop.lookingsat.repo.Transmitter
import retrofit2.http.GET

interface TransmitterApi {

    @GET("transmitters/?alive=true")
    suspend fun getTransmitterList(): List<Transmitter>
}