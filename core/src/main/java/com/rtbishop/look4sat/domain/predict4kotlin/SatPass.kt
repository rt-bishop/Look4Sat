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
package com.rtbishop.look4sat.domain.predict4kotlin

import java.util.*

data class SatPass(
    val catNum: Int,
    val name: String,
    val isDeepSpace: Boolean,
    private val aosTime: Long,
    val aosAzimuth: Double,
    private val losTime: Long,
    val losAzimuth: Double,
    private val tcaTime: Long,
    val tcaAzimuth: Double,
    val altitude: Double,
    val maxElevation: Double,
    val predictor: PassPredictor,
    var progress: Int = 0
) {
    val aosDate: Date
        get() = Date(aosTime)

    val losDate: Date
        get() = Date(losTime)

    val tcaDate: Date
        get() = Date(tcaTime)
}
