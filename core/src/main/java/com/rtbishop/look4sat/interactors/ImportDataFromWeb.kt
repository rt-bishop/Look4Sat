package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.data.SatelliteRepo

class ImportDataFromWeb(private val satelliteRepo: SatelliteRepo) {

    suspend operator fun invoke(sources: List<String>) {
        satelliteRepo.importDataFromWeb(sources)
    }
}
