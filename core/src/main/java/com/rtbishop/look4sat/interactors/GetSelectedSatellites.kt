package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.predict4kotlin.Satellite

class GetSelectedSatellites(private val satelliteRepo: SatelliteRepo) {

    suspend operator fun invoke(): List<Satellite> {
        return satelliteRepo.getSelectedSatellites()
    }
}