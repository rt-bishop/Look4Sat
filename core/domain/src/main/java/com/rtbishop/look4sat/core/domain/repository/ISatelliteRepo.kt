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
package com.rtbishop.look4sat.core.domain.repository

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import kotlinx.coroutines.flow.StateFlow

interface ISatelliteRepo {
    /** All selected OrbitalObjects, updated when the selection changes. */
    val satellites: StateFlow<List<OrbitalObject>>

    /** Raw calculated passes (without live progress). Updated on selection/filter change. */
    val passes: StateFlow<List<OrbitalPass>>

    /** Load satellite objects from DB based on the current selection. */
    suspend fun initRepository()

    /** Recalculate passes with the given filter parameters. */
    suspend fun calculatePasses(time: Long, hoursAhead: Int, minElevation: Double, modes: List<String>)

    /** Get the current position of a single satellite. */
    suspend fun getPosition(sat: OrbitalObject, pos: GeoPos, time: Long): OrbitalPos

    /** Get the ground track for a satellite over a time range. */
    suspend fun getTrack(sat: OrbitalObject, pos: GeoPos, start: Long, end: Long): List<OrbitalPos>

    /** Get Doppler-shifted radio frequencies for a satellite at the given time. */
    suspend fun getRadios(sat: OrbitalObject, pos: GeoPos, radios: List<SatRadio>, time: Long): List<SatRadio>

    /** Fetch radio transceivers for a satellite by its catalog number. */
    suspend fun getRadiosWithId(id: Int): List<SatRadio>
}
