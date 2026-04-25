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
package com.rtbishop.look4sat.feature.map

import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos

data class MapState(
    val mapData: MapData? = null,
    val isLightUi: Boolean = false,
    val isUtc: Boolean = false,
    val stationPosition: GeoPos? = null,
    val orbitalPass: OrbitalPass,
    val track: List<List<GeoPos>>? = null,
    val footprint: OrbitalPos? = null,
    val positions: Map<OrbitalObject, GeoPos>? = null,
    val sunLatDeg: Double = 0.0,
    val sunLonDeg: Double = 0.0,
    val moonLatDeg: Double = 0.0,
    val moonLonDeg: Double = 0.0
)

sealed interface MapAction {
    data object SelectPrev : MapAction
    data object SelectNext : MapAction
    data class SelectItem(val item: OrbitalObject) : MapAction
    data class SelectDefaultItem(val catnum: Int) : MapAction
}

data class MapData(
    val catNum: Int,
    val name: String,
    val aosTime: String,
    val isTimeAos: Boolean,
    val azimuth: Double,
    val elevation: Double,
    val range: Double,
    val altitude: Double,
    val velocity: Double,
    val qthLoc: String,
    val osmPos: GeoPos,
    val period: Double,
    val phase: Double,
    val eclipsed: Boolean
)
