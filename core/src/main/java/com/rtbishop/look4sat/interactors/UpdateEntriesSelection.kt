package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo

class UpdateEntriesSelection(private val satelliteRepo: SatelliteRepo) {

    suspend operator fun invoke(catNums: List<Int>, isSelected: Boolean) {
        satelliteRepo.updateEntriesSelection(catNums, isSelected)
    }
}