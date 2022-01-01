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

interface PreferencesHandler {

    val defaultSources: List<String>
        get() = listOf(
            "https://prismnet.com/~mmccants/tles/inttles.zip",
            "https://prismnet.com/~mmccants/tles/classfd.zip",
            "https://celestrak.com/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
            "https://amsat.org/tle/current/nasabare.txt"
        )
    val transmittersSource: String
        get() = "https://db.satnogs.org/api/transmitters/?format=json"

    fun loadDataSources(): List<String>

    fun saveDataSources(sources: List<String>)
}
