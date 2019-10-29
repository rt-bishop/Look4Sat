package com.rtbishop.lookingsat

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.rtbishop.lookingsat.api.RemoteDataSource
import com.rtbishop.lookingsat.api.TransmittersApi
import com.rtbishop.lookingsat.db.LocalDataSource
import com.rtbishop.lookingsat.db.TransmittersDatabase
import com.rtbishop.lookingsat.repo.Repository
import com.rtbishop.lookingsat.vm.ViewModelFactory
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
        val dao = TransmittersDatabase.getInstance(context).transmittersDao()

        return LocalDataSource(dao)
    }

    private fun provideRepository(context: Context): Repository {
        return Repository(provideLocalDataSource(context), provideRemoteDataSource())
    }

    private fun provideViewModelFactory(context: Context): ViewModelProvider.Factory {
        return ViewModelFactory(context, provideRepository(context))
    }
}