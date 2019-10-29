package com.rtbishop.lookingsat

import com.rtbishop.lookingsat.api.RemoteDataSource
import com.rtbishop.lookingsat.api.TransmittersApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Injector {
    private fun provideRemoteDataSource(): RemoteDataSource {
        val api = Retrofit.Builder()
            .baseUrl("https://db.satnogs.org/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TransmittersApi::class.java)

        val client = OkHttpClient()

        return RemoteDataSource(api, client)
    }
}