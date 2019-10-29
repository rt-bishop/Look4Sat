package com.rtbishop.lookingsat.db

import com.rtbishop.lookingsat.repo.Transmitter

class LocalDataSource(private val transmittersDao: TransmittersDao) {

    suspend fun insert(transmitters: List<Transmitter>) {
        transmittersDao.insert(transmitters)
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return transmittersDao.getTransmittersForSat(id)
    }
}