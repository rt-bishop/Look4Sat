package com.rtbishop.look4sat.domain.repository

import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import kotlinx.coroutines.flow.StateFlow

interface ISatelliteRepo {
    val passes: StateFlow<List<SatPass>>
    val satellites: StateFlow<List<Satellite>>
    suspend fun getRadiosWithId(id: Int): List<SatRadio>
    suspend fun initRepository()
    suspend fun getPosition(sat: Satellite, pos: GeoPos, time: Long): SatPos
    suspend fun getTrack(sat: Satellite, pos: GeoPos, start: Long, end: Long): List<SatPos>
    suspend fun getRadios(sat: Satellite, pos: GeoPos, radios: List<SatRadio>, time: Long): List<SatRadio>
    suspend fun processPasses(passList: List<SatPass>, time: Long): List<SatPass>
    suspend fun calculatePasses(time: Long, hoursAhead: Int, minElevation: Double, modes: List<String>)
}
