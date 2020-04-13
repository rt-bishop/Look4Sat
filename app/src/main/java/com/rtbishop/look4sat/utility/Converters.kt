package com.rtbishop.look4sat.utility

import androidx.room.TypeConverter
import com.github.amsacode.predict4java.TLE
import com.google.gson.Gson

class Converters {
    private val gSon = Gson()

    @TypeConverter
    fun tleToString(tle: TLE): String {
        return gSon.toJson(tle)
    }

    @TypeConverter
    fun tleFromString(string: String): TLE {
        return gSon.fromJson(string, TLE::class.java)
    }
}
