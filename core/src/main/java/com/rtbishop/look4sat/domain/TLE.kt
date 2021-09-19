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
package com.rtbishop.look4sat.domain

import java.io.InputStream
import kotlin.math.pow

data class TLE(
    val name: String,
    val epoch: Double,
    val meanmo: Double,
    val eccn: Double,
    val incl: Double,
    val raan: Double,
    val argper: Double,
    val meanan: Double,
    val catnum: Int,
    val bstar: Double
) {
    val xincl: Double = Math.toRadians(incl)
    val xnodeo: Double = Math.toRadians(raan)
    val omegao: Double = Math.toRadians(argper)
    val xmo: Double = Math.toRadians(meanan)
    val xno: Double = meanmo * Math.PI * 2.0 / 1440
    val isDeepspace: Boolean = meanmo < 6.4

    fun createSat(): Satellite {
        return when {
            this.isDeepspace -> DeepSpaceSat(this)
            else -> NearEarthSat(this)
        }
    }

    companion object {

        fun parseTleStream(stream: InputStream): List<TLE> {
            val tleStrings = mutableListOf(String(), String(), String())
            val parsedItems = mutableListOf<TLE>()
            var lineIndex = 0
            stream.bufferedReader().forEachLine { line ->
                tleStrings[lineIndex] = line
                if (lineIndex < 2) {
                    lineIndex++
                } else {
                    val isLineOneValid = tleStrings[1].substring(0, 1) == "1"
                    val isLineTwoValid = tleStrings[2].substring(0, 1) == "2"
                    if (!isLineOneValid && !isLineTwoValid) return@forEachLine
                    parseTleStrings(tleStrings)?.let { tle -> parsedItems.add(tle) }
                    lineIndex = 0
                }
            }
            return parsedItems
        }

        private fun parseTleStrings(tleStrings: List<String>): TLE? {
            if (tleStrings[1].substring(0, 1) != "1" && tleStrings[2].substring(0, 1) != "2") {
                return null
            }
            try {
                val name: String = tleStrings[0].trim()
                val epoch: Double = tleStrings[1].substring(18, 32).toDouble()
                val meanmo: Double = tleStrings[2].substring(52, 63).toDouble()
                val eccn: Double = 1.0e-07 * tleStrings[2].substring(26, 33).toDouble()
                val incl: Double = tleStrings[2].substring(8, 16).toDouble()
                val raan: Double = tleStrings[2].substring(17, 25).toDouble()
                val argper: Double = tleStrings[2].substring(34, 42).toDouble()
                val meanan: Double = tleStrings[2].substring(43, 51).toDouble()
                val catnum: Int = tleStrings[1].substring(2, 7).trim().toInt()
                val bstar: Double = 1.0e-5 * tleStrings[1].substring(53, 59).toDouble() /
                        10.0.pow(tleStrings[1].substring(60, 61).toDouble())
                return TLE(name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar)
            } catch (exception: Exception) {
                return null
            }
        }
    }
}
