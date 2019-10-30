package com.rtbishop.lookingsat.repo

import android.util.Log
import com.rtbishop.lookingsat.api.RemoteDataSource
import com.rtbishop.lookingsat.db.LocalDataSource
import java.io.IOException
import java.io.InputStream

class Repository(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource
) {
    fun fetchTleStream(): InputStream {
        return remoteSource.fetchTleStream()
    }

    suspend fun updateTransmittersDatabase() {
        try {
            localSource.insertTransmitters(remoteSource.fetchTransmittersList())
        } catch (exception: IOException) {
            Log.d(this.javaClass.simpleName, exception.toString())
        }
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return localSource.getTransmittersForSat(id)
    }
}