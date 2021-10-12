/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.GeoPos

interface Preferences {

    fun positionToQTH(lat: Double, lon: Double): String?

    fun loadStationPosition(): GeoPos

    fun saveStationPosition(pos: GeoPos)

    fun updatePositionFromGPS(): Boolean

    fun updatePositionFromQTH(qthString: String): Boolean

    fun getMagDeclination(): Float

    fun getHoursAhead(): Int

    fun getMinElevation(): Double

    fun shouldUseTextLabels(): Boolean

    fun shouldUseUTC(): Boolean

    fun shouldUseCompass(): Boolean

    fun shouldShowSweep(): Boolean

    fun isSetupDone(): Boolean

    fun setSetupDone()

    fun saveModesSelection(modes: List<String>)

    fun loadModesSelection(): List<String>

    fun getRotatorServer(): Pair<String, Int>?

    fun loadTleSources(): List<String>

    fun saveTleSources(sources: List<String>)

    fun loadDefaultSources(): List<String>
}
