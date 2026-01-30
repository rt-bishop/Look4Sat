/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
