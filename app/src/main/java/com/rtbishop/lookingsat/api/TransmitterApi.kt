package com.rtbishop.lookingsat.api

import com.rtbishop.lookingsat.repo.Transmitter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface TransmitterApi {

    @GET("transmitters/?alive=true")
    suspend fun getTransmitterList(): List<Transmitter>

    companion object {
        operator fun invoke(): TransmitterApi {
            return Retrofit.Builder()
                .baseUrl("https://db.satnogs.org/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TransmitterApi::class.java)
        }
    }
}