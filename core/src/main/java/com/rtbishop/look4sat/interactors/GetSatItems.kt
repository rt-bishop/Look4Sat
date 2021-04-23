package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.domain.model.SatItem
import kotlinx.coroutines.flow.Flow

class GetSatItems(private val satelliteRepo: SatelliteRepo) {

    operator fun invoke(): Flow<List<SatItem>> {
        return satelliteRepo.getSatItems()
    }
}
