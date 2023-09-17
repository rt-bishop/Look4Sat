package com.rtbishop.look4sat.domain.repository

import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.OrbitalObject
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.predict.OrbitalPos
import kotlinx.coroutines.flow.StateFlow

interface ISatelliteRepo {
    val passes: StateFlow<List<OrbitalPass>>
    val satellites: StateFlow<List<OrbitalObject>>
    suspend fun getRadiosWithId(id: Int): List<SatRadio>
    suspend fun initRepository()
    suspend fun getPosition(sat: OrbitalObject, pos: GeoPos, time: Long): OrbitalPos
    suspend fun getTrack(sat: OrbitalObject, pos: GeoPos, start: Long, end: Long): List<OrbitalPos>
    suspend fun getRadios(sat: OrbitalObject, pos: GeoPos, radios: List<SatRadio>, time: Long): List<SatRadio>
    suspend fun processPasses(passList: List<OrbitalPass>, time: Long): List<OrbitalPass>
    suspend fun calculatePasses(time: Long, hoursAhead: Int, minElevation: Double, modes: List<String>)
}
