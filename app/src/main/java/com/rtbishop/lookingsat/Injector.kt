package com.rtbishop.lookingsat

import android.content.Context
import com.rtbishop.lookingsat.network.RemoteDataSource
import com.rtbishop.lookingsat.network.TransmittersApi
import com.rtbishop.lookingsat.repo.Repository
import com.rtbishop.lookingsat.storage.LocalDataSource
import com.rtbishop.lookingsat.storage.TransmittersDatabase
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

    private fun provideLocalDataSource(context: Context): LocalDataSource {
        return LocalDataSource(TransmittersDatabase.getInstance(context).transmittersDao())
    }

    fun provideRepository(context: Context): Repository {
        return Repository(provideLocalDataSource(context), provideRemoteDataSource())
    }
}