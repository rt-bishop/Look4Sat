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
    private val alpha5Alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val celestrakCSVColumns = mapOf(
        "OBJECT_NAME" to 0, "EPOCH" to 2, "MEAN_MOTION" to 3, "ECCENTRICITY" to 4,
        "INCLINATION" to 5, "RA_OF_ASC_NODE" to 6, "ARG_OF_PERICENTER" to 7,
        "MEAN_ANOMALY" to 8, "NORAD_CAT_ID" to 11, "BSTAR" to 14, "MEAN_MOTION_DOT" to 15
    )

    suspend fun parseCSVStream(stream: InputStream): List<OrbitalData> = withContext(dispatcher) {
        stream.bufferedReader().useLines { lines ->
            val iterator = lines.iterator()
            if (!iterator.hasNext()) return@useLines emptyList()
            val columns = parseCSVColumns(iterator.next())
            iterator.asSequence().mapNotNull { line -> parseCSV(line.split(","), columns) }.toList()
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

    private fun parseCSV(values: List<String>, columns: Map<String, Int>): OrbitalData? = runCatching {
        fun value(column: String) = values[columns.getValue(column)].trim()
        fun optionalValue(column: String) =
            columns[column]?.let { index -> values.getOrNull(index) }?.trim()?.toDoubleOrNull() ?: 0.0
        OrbitalData(
            name = value("OBJECT_NAME"),
            epoch = parseTimestamp(value("EPOCH")),
            meanmo = value("MEAN_MOTION").toDouble(),
            eccn = value("ECCENTRICITY").toDouble(),
            incl = value("INCLINATION").toDouble(),
            raan = value("RA_OF_ASC_NODE").toDouble(),
            argper = value("ARG_OF_PERICENTER").toDouble(),
            meanan = value("MEAN_ANOMALY").toDouble(),
            catnum = parseCatnum(value("NORAD_CAT_ID")),
            bstar = optionalValue("BSTAR"),
            ndot = optionalValue("MEAN_MOTION_DOT")
        )
    }.onFailure { println("CSV parsing exception: $it") }.getOrNull()

    /**
     * OMM columns are located by name, as providers agree on the names but not on the order.
     * Falls back to the Celestrak layout when the header is missing or unrecognized.
     */
    private fun parseCSVColumns(header: String): Map<String, Int> {
        val columns = header.split(",").withIndex().associate { (index, name) ->
            name.trim().trim('"').uppercase() to index
        }
        return if (columns.containsKey("NORAD_CAT_ID")) columns else celestrakCSVColumns
    }

    /**
     * ISO 8601 timestamp as a TLE style YYDDD.ffffffff epoch. Fractional seconds are optional and
     * a trailing timezone marker is ignored, as not every provider formats the epoch alike.
     */
    private fun parseTimestamp(timestamp: String): Double {
        val year = timestamp.substring(0, 4).toInt()
        val month = timestamp.substring(5, 7).toInt()
        val dayOfMonth = timestamp.substring(8, 10).toInt()
        val hours = timestamp.substring(11, 13).toInt()
        val minutes = timestamp.substring(14, 16).toInt()
        val seconds = timestamp.substring(17).takeWhile { it.isDigit() || it == '.' }.toDouble()
        val dayFraction = (hours * 3600 + minutes * 60 + seconds) / 86400.0
        return (year % 100) * 1000 + getDayOfYear(year, month, dayOfMonth) + dayFraction
    }

    /**
     * Catalog numbers above 99999 do not fit the 5 digit TLE field, so they are encoded as Alpha-5:
     * the leading two digits become a letter, with I and O skipped to avoid confusion with 1 and 0.
     */
    private fun parseCatnum(value: String): Int {
        val catnum = value.trim()
        if (catnum.first().isDigit()) return catnum.toInt()
        val alphaIndex = alpha5Alphabet.indexOf(catnum.first().uppercaseChar())
        require(alphaIndex >= 0) { "Unknown Alpha-5 catalog number: $catnum" }
        return (alphaIndex + 10) * 10000 + catnum.drop(1).trim().toInt()
    }

    private fun parseTLE(tle: List<String>): OrbitalData? = runCatching {
        val line1 = tle[1]
        val line2 = tle[2]
        OrbitalData(
            name = tle[0].trim().removePrefix("0 "),
            epoch = line1.substring(18, 32).toDouble(),
            meanmo = line2.substring(52, 63).toDouble(),
            eccn = line2.substring(26, 33).toDouble() / 1e7,
            incl = line2.substring(8, 16).toDouble(),
            raan = line2.substring(17, 25).toDouble(),
            argper = line2.substring(34, 42).toDouble(),
            meanan = line2.substring(43, 51).toDouble(),
            catnum = parseCatnum(line1.substring(2, 7)),
            bstar = 1e-5 * line1.substring(53, 59).toDouble() / 10.0.pow(line1.substring(60, 61).toDouble()),
            ndot = line1.substring(33, 43).trim().toDouble()
        )
    }.onFailure { println("TLE parsing exception: $it") }.getOrNull()

    fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    fun getDayOfYear(year: Int, month: Int, dayOfMonth: Int): Int {
        val daysInMonth = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return daysInMonth.take(month - 1).sum() + dayOfMonth
    }
}
