/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.domain.predict

data class OrbitalData(
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
    val xincl: Double = incl * DEG2RAD
    val xnodeo: Double = raan * DEG2RAD
    val omegao: Double = argper * DEG2RAD
    val xmo: Double = meanan * DEG2RAD
    val xno: Double = meanmo * TWO_PI / MIN_PER_DAY
    val orbitalPeriod: Double = MIN_PER_DAY / meanmo
    val isDeepSpace: Boolean = orbitalPeriod >= 225.0 // NearEarth (period < 225 min) or DeepSpace (period >= 225 min)
    fun getObject(): OrbitalObject = if (isDeepSpace) DeepSpaceObject(this) else NearEarthObject(this)
}
