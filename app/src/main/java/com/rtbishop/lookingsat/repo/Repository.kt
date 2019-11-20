package com.rtbishop.lookingsat.repo

import com.rtbishop.lookingsat.network.RemoteDataSource
import com.rtbishop.lookingsat.storage.LocalDataSource
import java.io.InputStream

class Repository(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource
) {
    fun fetchTleStream(): InputStream {
        return remoteSource.fetchTleStream()
    }

    suspend fun updateTransmittersDatabase() {
        localSource.insertTransmitters(remoteSource.fetchTransmittersList())
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return localSource.getTransmittersForSat(id)
    }
}