/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.domain.utility

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.InputStream
import kotlin.math.pow

class DataParser(private val dispatcher: CoroutineDispatcher) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun parseCSVStream(stream: InputStream): List<OrbitalData> = withContext(dispatcher) {
        stream.bufferedReader().useLines { lines ->
            lines.drop(1).mapNotNull { parseCSV(it.split(",")) }.toList()
        }
    }

    suspend fun parseTLEStream(stream: InputStream): List<OrbitalData> = withContext(dispatcher) {
        stream.bufferedReader().readLines()
            .chunked(3)
            .filter { it.size == 3 && it[1].startsWith("1") && it[2].startsWith("2") }
            .mapNotNull { parseTLE(it) }
    }

    suspend fun parseJSONStream(stream: InputStream): List<SatRadio> = withContext(dispatcher) {
        runCatching {
            val root = json.parseToJsonElement(stream.bufferedReader().readText())
            (root as? JsonArray)?.mapNotNull { element ->
                runCatching { json.decodeFromJsonElement<SatRadio>(element) }
                    .onFailure { println("JSON parsing exception: $it") }
                    .getOrNull()
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun parseCSV(values: List<String>): OrbitalData? = runCatching {
        val name = values[0]
        val timestamp = values[2]
        val year = timestamp.substring(0, 4)
        val month = timestamp.substring(5, 7).toInt()
        val dayOfMonth = timestamp.substring(8, 10).toInt()
        val dayInt = getDayOfYear(year.toInt(), month, dayOfMonth)
        val day = dayInt.toString().padStart(3, '0')
        val hour = timestamp.substring(11, 13).toInt() * 3600000
        val min = timestamp.substring(14, 16).toInt() * 60000
        val sec = timestamp.substring(17, 19).toInt() * 1000
        val ms = timestamp.substring(20, 26).toInt() / 1000.0
        val frac = ((hour + min + sec + ms) / 86400000.0).toString().substring(1)
        val epoch = "${year.substring(2)}$day$frac".toDouble()
        OrbitalData(
            name = name,
            epoch = epoch,
            meanmo = values[3].toDouble(),
            eccn = values[4].toDouble(),
            incl = values[5].toDouble(),
            raan = values[6].toDouble(),
            argper = values[7].toDouble(),
            meanan = values[8].toDouble(),
            catnum = values[11].toInt(),
            bstar = values[14].toDouble()
        )
    }.onFailure { println("CSV parsing exception: $it") }.getOrNull()

    private fun parseTLE(tle: List<String>): OrbitalData? = runCatching {
        val line1 = tle[1]
        val line2 = tle[2]
        OrbitalData(
            name = tle[0].trim(),
            epoch = line1.substring(18, 32).toDouble(),
            meanmo = line2.substring(52, 63).toDouble(),
            eccn = line2.substring(26, 33).toDouble() / 1e7,
            incl = line2.substring(8, 16).toDouble(),
            raan = line2.substring(17, 25).toDouble(),
            argper = line2.substring(34, 42).toDouble(),
            meanan = line2.substring(43, 51).toDouble(),
            catnum = line1.substring(2, 7).trim().toInt(),
            bstar = 1e-5 * line1.substring(53, 59).toDouble() / 10.0.pow(line1.substring(60, 61).toDouble())
        )
    }.onFailure { println("TLE parsing exception: $it") }.getOrNull()

    private fun getDayOfYear(year: Int, month: Int, dayOfMonth: Int): Int {
        val daysInMonth = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return daysInMonth.take(month - 1).sum() + dayOfMonth
    }
}
