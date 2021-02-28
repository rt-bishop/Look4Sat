/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.utility

import androidx.room.TypeConverter
import com.github.amsacode.predict4java.TLE
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

object TypeConverters {
    
    private lateinit var tleAdapter: JsonAdapter<TLE>
    
    fun initialize(moshi: Moshi) {
        tleAdapter = moshi.adapter(TLE::class.java)
    }
    
    @JvmStatic
    @TypeConverter
    fun tleToString(tle: TLE): String {
        return tleAdapter.toJson(tle)
    }
    
    @JvmStatic
    @TypeConverter
    fun tleFromString(string: String): TLE? {
        return tleAdapter.fromJson(string)
    }
}
