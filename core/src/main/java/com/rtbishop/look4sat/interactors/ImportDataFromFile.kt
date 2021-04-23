package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo
import java.io.InputStream

class ImportDataFromFile(private val satelliteRepo: SatelliteRepo) {

    suspend operator fun invoke(stream: InputStream) {
        satelliteRepo.importDataFromFile(stream)
    }
}
