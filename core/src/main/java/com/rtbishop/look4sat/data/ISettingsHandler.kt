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

import com.rtbishop.look4sat.domain.predict.GeoPos

interface ISettingsHandler {

    val defaultSources: List<String>
        get() = listOf(
            "https://www.prismnet.com/~mmccants/tles/inttles.zip",
            "https://www.prismnet.com/~mmccants/tles/classfd.zip",
            "https://celestrak.com/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
            "https://amsat.org/tle/current/nasabare.txt"
        )

    fun loadStationLocator(): String

    fun saveStationLocator(locator: String)

    fun loadStationPosition(): GeoPos

    fun saveStationPosition(position: GeoPos)

    fun getHoursAhead(): Int

    fun setHoursAhead(hoursAhead: Int)

    fun getMinElevation(): Double

    fun setMinElevation(minElevation: Double)

    fun getUseUTC(): Boolean

    fun setUseUTC(value: Boolean)

    fun getUseCompass(): Boolean

    fun setUseCompass(value: Boolean)

    fun getShowSweep(): Boolean

    fun setShowSweep(value: Boolean)

    fun saveModesSelection(modes: List<String>)

    fun loadModesSelection(): List<String>

    fun saveEntriesSelection(catnums: List<Int>)

    fun loadEntriesSelection(): List<Int>

    fun getRotatorEnabled(): Boolean

    fun setRotatorEnabled(value: Boolean)

    fun getRotatorServer(): String

    fun setRotatorServer(value: String)

    fun getRotatorPort(): String

    fun setRotatorPort(value: String)

    fun loadDataSources(): List<String>

    fun saveDataSources(sources: List<String>)
}
