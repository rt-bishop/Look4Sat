package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.data.SatelliteRepo
import com.rtbishop.look4sat.domain.model.SatTrans
import kotlinx.coroutines.flow.Flow

class GetSatTransmitters(private val satelliteRepo: SatelliteRepo) {

    operator fun invoke(catNum: Int): Flow<List<SatTrans>> {
        return satelliteRepo.getSatTransmitters(catNum)
    }
}
