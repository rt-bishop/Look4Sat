package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.domain.model.SatTrans
import kotlinx.coroutines.flow.Flow

class GetTransmittersForSat(private val satelliteRepo: SatelliteRepo) {

    operator fun invoke(catNum: Int): Flow<List<SatTrans>> {
        return satelliteRepo.getTransmittersForSat(catNum)
    }
}