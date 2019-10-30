package com.rtbishop.lookingsat.db

import com.rtbishop.lookingsat.repo.Transmitter

class LocalDataSource(private val transmittersDao: TransmittersDao) {

    suspend fun insertTransmitters(transmitters: List<Transmitter>) {
        transmittersDao.insertTransmitters(transmitters)
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return transmittersDao.getTransmittersForSat(id)
    }
}