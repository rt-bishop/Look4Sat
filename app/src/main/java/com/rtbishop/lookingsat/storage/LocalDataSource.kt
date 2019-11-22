package com.rtbishop.lookingsat.storage

import com.rtbishop.lookingsat.repo.Transmitter
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val transmittersDao: TransmittersDao) {

    suspend fun insertTransmitters(transmitters: List<Transmitter>) {
        transmittersDao.insertTransmitters(transmitters)
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return transmittersDao.getTransmittersForSat(id)
    }
}