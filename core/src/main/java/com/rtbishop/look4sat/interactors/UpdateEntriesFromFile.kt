package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.data.SatelliteRepo
import java.io.InputStream

class UpdateEntriesFromFile(private val satelliteRepo: SatelliteRepo) {

    suspend operator fun invoke(stream: InputStream) {
        satelliteRepo.updateEntriesFromFile(stream)
    }
}
