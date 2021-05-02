package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.domain.SatelliteRepo
import java.io.InputStream

class ImportDataFromStream(private val satelliteRepo: SatelliteRepo) {

    suspend operator fun invoke(stream: InputStream) {
        satelliteRepo.importDataFromStream(stream)
    }
}
