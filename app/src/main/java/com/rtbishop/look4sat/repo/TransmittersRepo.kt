package com.rtbishop.look4sat.repo

import androidx.lifecycle.LiveData
import com.rtbishop.look4sat.data.SatTrans

interface TransmittersRepo {

    suspend fun updateTransmitters()

    fun getTransmittersForSat(satId: Int): LiveData<List<SatTrans>>
}