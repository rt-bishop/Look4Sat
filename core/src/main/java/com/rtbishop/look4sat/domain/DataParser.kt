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

import com.rtbishop.look4sat.domain.model.Transmitter
import com.rtbishop.look4sat.domain.predict.TLE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import kotlin.math.pow

class DataParser(private val parserDispatcher: CoroutineDispatcher) {

    suspend fun parseCSVStream(csvStream: InputStream): List<TLE> = withContext(parserDispatcher) {
        val parsedItems = mutableListOf<TLE>()
        csvStream.bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (index != 0) {
                    val values = line.split(",")
                    parseCSV(values)?.let { tle -> parsedItems.add(tle) }
                }
            }
        }
        return@withContext parsedItems
    }

    suspend fun parseTLEStream(tleStream: InputStream): List<TLE> = withContext(parserDispatcher) {
        val tleStrings = mutableListOf(String(), String(), String())
        val parsedItems = mutableListOf<TLE>()
        var lineIndex = 0
        tleStream.bufferedReader().forEachLine { line ->
            tleStrings[lineIndex] = line
            if (lineIndex < 2) {
                lineIndex++
            } else {
                val isLineOneValid = tleStrings[1].substring(0, 1) == "1"
                val isLineTwoValid = tleStrings[2].substring(0, 1) == "2"
                if (!isLineOneValid && !isLineTwoValid) return@forEachLine
                parseTLE(tleStrings)?.let { tle -> parsedItems.add(tle) }
                lineIndex = 0
            }
        }
        return@withContext parsedItems
    }

    suspend fun parseJSONStream(jsonStream: InputStream): List<Transmitter> {
        return withContext(parserDispatcher) {
            val parsedItems = mutableListOf<Transmitter>()
            try {
                val jsonArray = JSONArray(jsonStream.bufferedReader().readText())
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(index)
                    parseJSON(jsonObject)?.let { parsedItems.add(it) }
                }
                return@withContext parsedItems
            } catch (exception: Exception) {
                return@withContext parsedItems
            }
        }
    }

    private fun parseCSV(values: List<String>): TLE? {
        try {
            val name = values[0]
            val year = values[2].substring(0, 4)
            val month = values[2].substring(5, 7)
            val dayOfMonth = values[2].substring(8, 10)
            val day = getDayOfYear(year.toInt(), month.toInt(), dayOfMonth.toInt())
            val hour = values[2].substring(11, 13).toInt() * 3600000000 // microseconds in one hour
            val min = values[2].substring(14, 16).toInt() * 60000000 // microseconds in one minute
            val sec = values[2].substring(17, 19).toInt() * 1000000 // microseconds in one second
            val microsec = values[2].substring(20, 26).toInt()
            val frac = ((hour + min + sec + microsec) / 86400000000.0).toString()
            val epoch = "${year.substring(2)}$day${frac.substring(1)}".toDouble()
            val meanmo = values[3].toDouble()
            val eccn = values[4].toDouble()
            val incl = values[5].toDouble()
            val raan = values[6].toDouble()
            val argper = values[7].toDouble()
            val meanan = values[8].toDouble()
            val catnum = values[11].toInt()
            val bstar = values[14].toDouble()
            return TLE(name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar)
        } catch (exception: Exception) {
            return null
        }
    }

    private fun parseTLE(tle: List<String>): TLE? {
        if (tle[1].substring(0, 1) != "1" && tle[2].substring(0, 1) != "2") {
            return null
        }
        try {
            val name: String = tle[0].trim()
            val epoch: Double = tle[1].substring(18, 32).toDouble()
            val meanmo: Double = tle[2].substring(52, 63).toDouble()
            val eccn: Double = tle[2].substring(26, 33).toDouble() / 10000000.0
            val incl: Double = tle[2].substring(8, 16).toDouble()
            val raan: Double = tle[2].substring(17, 25).toDouble()
            val argper: Double = tle[2].substring(34, 42).toDouble()
            val meanan: Double = tle[2].substring(43, 51).toDouble()
            val catnum: Int = tle[1].substring(2, 7).trim().toInt()
            val bstar: Double = 1.0e-5 * tle[1].substring(53, 59).toDouble() /
                    10.0.pow(tle[1].substring(60, 61).toDouble())
            return TLE(name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar)
        } catch (exception: Exception) {
            return null
        }
    }

    private fun parseJSON(json: JSONObject): Transmitter? {
        try {
            val uuid = json.getString("uuid")
            val info = json.getString("description")
            val isAlive = json.getBoolean("alive")
            val downlink = if (json.isNull("downlink_low")) null
            else json.getLong("downlink_low")
            val uplink = if (json.isNull("uplink_low")) null
            else json.getLong("uplink_low")
            val mode = if (json.isNull("mode")) null
            else json.getString("mode")
            val isInverted = json.getBoolean("invert")
            val catnum = if (json.isNull("norad_cat_id")) null
            else json.getInt("norad_cat_id")
            return Transmitter(uuid, info, isAlive, downlink, uplink, mode, isInverted, catnum)
        } catch (exception: Exception) {
            return null
        }
    }

    private fun getDayOfYear(year: Int, month: Int, dayOfMonth: Int): Int {
        if (month == 1) return dayOfMonth
        val daysArray = arrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var dayOfYear = dayOfMonth
        // If leap year increment Feb days
        if (((year / 4 == 0) && (year / 100 != 0)) || (year / 400 == 0)) daysArray[1]++
        for (i in 0 until month - 1) {
            dayOfYear += daysArray[i]
        }
        return dayOfYear
    }
}
