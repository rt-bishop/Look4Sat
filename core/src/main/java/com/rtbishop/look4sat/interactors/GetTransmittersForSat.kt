package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.domain.model.Transmitter
import kotlinx.coroutines.flow.Flow

class GetTransmittersForSat(private val satelliteRepo: SatelliteRepo) {

    operator fun invoke(catNum: Int): Flow<List<Transmitter>> {
        return satelliteRepo.getTransmittersForSat(catNum)
    }
}