package com.rtbishop.lookingsat

import android.content.Context
import com.rtbishop.lookingsat.api.RemoteDataSource
import com.rtbishop.lookingsat.api.TransmittersApi
import com.rtbishop.lookingsat.db.LocalDataSource
import com.rtbishop.lookingsat.db.TransmittersDatabase
import com.rtbishop.lookingsat.repo.Repository
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