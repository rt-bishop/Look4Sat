package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.model.SatPass
import com.rtbishop.look4sat.model.SatPos
import com.rtbishop.look4sat.model.SatRadio
import kotlinx.coroutines.flow.SharedFlow

interface ISatelliteRepo {

    val calculatedPasses: SharedFlow<List<SatPass>>

    fun getPasses(): List<SatPass>

    suspend fun getEntriesWithIds(ids: List<Int>): List<Satellite>

    suspend fun getRadiosWithId(id: Int): List<SatRadio>

    suspend fun getPosition(sat: Satellite, pos: GeoPos, time: Long): SatPos

    suspend fun getTrack(sat: Satellite, pos: GeoPos, start: Long, end: Long): List<SatPos>

    suspend fun processRadios(
        sat: Satellite,
        pos: GeoPos,
        radios: List<SatRadio>,
        time: Long
    ): List<SatRadio>

    suspend fun processPasses(passList: List<SatPass>, time: Long): List<SatPass>

    suspend fun calculatePasses(
        satList: List<Satellite>,
        pos: GeoPos,
        time: Long,
        hoursAhead: Int = 8,
        minElevation: Double = 16.0
    )
}
