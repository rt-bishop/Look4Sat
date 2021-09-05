package com.rtbishop.look4sat.domain.predict4kotlin

import java.io.InputStream
import kotlin.math.pow

object SatelliteFactory {

    fun createSat(tle: TLE?): Satellite? {
        return when {
            tle == null -> null
            tle.isDeepspace -> DeepSpaceSat(tle)
            else -> NearEarthSat(tle)
        }
    }

    fun createSat(array: Array<String>): Satellite? {
        val importedElement = importElement(array)
        return createSat(importedElement)
    }

    fun createDummySat(): Satellite? {
        val elementArray = arrayOf(
            "ISS (ZARYA)",
            "1 25544U 98067A   21242.56000419  .00070558  00000-0  12956-2 0  9996",
            "2 25544  51.6433 334.9559 0003020 334.9496 106.9882 15.48593918300128"
        )
        return createSat(importElement(elementArray))
    }

    fun importElement(array: Array<String>): TLE? {
        if (array.size != 3) return null
        try {
            val name: String = array[0].trim()
            val epoch: Double = array[1].substring(18, 32).toDouble()
            val meanmo: Double = array[2].substring(52, 63).toDouble()
            val eccn: Double = 1.0e-07 * array[2].substring(26, 33).toDouble()
            val incl: Double = array[2].substring(8, 16).toDouble()
            val raan: Double = array[2].substring(17, 25).toDouble()
            val argper: Double = array[2].substring(34, 42).toDouble()
            val meanan: Double = array[2].substring(43, 51).toDouble()
            val catnum: Int = array[1].substring(2, 7).trim().toInt()
            val bstar: Double = 1.0e-5 * array[1].substring(53, 59).toDouble() /
                    10.0.pow(array[1].substring(60, 61).toDouble())
            return TLE(name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar)
        } catch (exception: Exception) {
            return null
        }
    }

    fun importElements(stream: InputStream): List<TLE> {
        val elementArray = arrayOf(String(), String(), String())
        val importedElements = mutableListOf<TLE>()
        var line = 0
        stream.bufferedReader().forEachLine {
            if (line != 2) {
                elementArray[line] = it
                line++
            } else {
                elementArray[line] = it
                importElement(elementArray)?.let { tle -> importedElements.add(tle) }
                line = 0
            }
        }
        return importedElements
    }
}