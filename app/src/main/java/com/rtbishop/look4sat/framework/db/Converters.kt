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
package com.rtbishop.look4sat.framework.db

import androidx.room.TypeConverter
import com.rtbishop.look4sat.domain.Satellite
import com.rtbishop.look4sat.domain.TLE
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

object Converters {

    private lateinit var paramsAdapter: JsonAdapter<TLE>

    fun initialize(moshi: Moshi) {
        paramsAdapter = moshi.adapter(TLE::class.java)
    }

    @JvmStatic
    @TypeConverter
    fun paramsToString(tle: TLE): String {
        return paramsAdapter.toJson(tle)
    }

    @JvmStatic
    @TypeConverter
    fun paramsFromString(string: String): TLE? {
        return paramsAdapter.fromJson(string)
    }

    @JvmStatic
    @TypeConverter
    fun satelliteFromString(string: String): Satellite? {
        return paramsAdapter.fromJson(string)?.createSat()
    }
}
