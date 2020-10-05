package com.rtbishop.look4sat.repo

import androidx.lifecycle.LiveData
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.repo.local.TransmittersDao
import com.rtbishop.look4sat.repo.remote.TransmittersApi
import javax.inject.Inject

class DefaultTransmittersRepo @Inject constructor(
    private val transmittersDao: TransmittersDao,
    private val transmittersApi: TransmittersApi
) : TransmittersRepo {

    override fun getTransmittersForSat(satId: Int): LiveData<List<SatTrans>> {
        return transmittersDao.getTransmittersForSat(satId)
    }

    override suspend fun updateTransmitters() {
        transmittersDao.updateTransmitters(transmittersApi.getTransmitters())
    }
}