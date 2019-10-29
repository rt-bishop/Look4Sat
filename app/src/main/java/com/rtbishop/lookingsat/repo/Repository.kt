package com.rtbishop.lookingsat.repo

import com.rtbishop.lookingsat.api.RemoteDataSource
import com.rtbishop.lookingsat.db.LocalDataSource

class Repository(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource
) {
    suspend fun fetchTles(): ByteArray {
        return remoteSource.fetchTles()
    }

    suspend fun updateTransmitters() {
        val transmitters = remoteSource.fetchTransmitters()
        localSource.insert(transmitters)
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        updateTransmitters()
        return localSource.getTransmittersForSat(id)
    }
}