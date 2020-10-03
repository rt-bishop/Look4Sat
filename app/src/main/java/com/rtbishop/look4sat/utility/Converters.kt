package com.rtbishop.look4sat.utility

import androidx.room.TypeConverter
import com.github.amsacode.predict4java.TLE
import com.squareup.moshi.Moshi

class Converters {
    private val jsonConverter = Moshi.Builder().build()
    private val jsonAdapter = jsonConverter.adapter(TLE::class.java)

    @TypeConverter
    fun tleToString(tle: TLE): String {
        return jsonAdapter.toJson(tle)
    }

    @TypeConverter
    fun tleFromString(string: String): TLE? {
        return jsonAdapter.fromJson(string)
    }
}
