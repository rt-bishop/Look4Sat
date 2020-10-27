package com.rtbishop.look4sat.utility

import android.content.SharedPreferences
import android.view.View
import com.github.amsacode.predict4java.GroundStationPosition
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.TimeUnit
import kotlin.math.round

object Utilities {

    fun formatForTimer(millis: Long): String {
        val format = "%02d:%02d:%02d"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(format, hours, minutes, seconds)
    }

    fun String.snack(view: View) {
        Snackbar.make(view, this, Snackbar.LENGTH_SHORT).show()
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    fun SharedPreferences.Editor.putDouble(key: String, double: Double) {
        putLong(key, double.toRawBits())
    }

    fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return Double.fromBits(getLong(key, default.toRawBits()))
    }

    fun qthToGSP(qthString: String): GroundStationPosition {
        val latFirst = (qthString[1].toInt() - 65) * 10
        val latSecond = qthString[3].toString().toInt()
        val latThird = (((qthString[5].toInt() - 97) / 24.0) + (1.0 / 48.0)) - 90
        val latitude = latFirst + latSecond + latThird

        val lonFirst = (qthString[0].toInt() - 65) * 20
        val lonSecond = qthString[2].toString().toInt() * 2
        val lonThird = (((qthString[4].toInt() - 97) / 12.0) + (1.0 / 24.0)) - 180
        val longitude = lonFirst + lonSecond + lonThird

        return GroundStationPosition(latitude, longitude, 0.0)
    }

    fun locToQTH(lat: Double, lon: Double): String {
        val tempLon = if (lon > 180.0) lon - 360 else lon
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWX"
        val lower = "abcdefghijklmnopqrstuvwx"

        val latitude = lat + 90
        val latFirst = upper[(latitude / 10).toInt()]
        val latSecond = (latitude % 10).toInt().toString()
        val latThird = lower[((latitude % 1) * 24).toInt()]

        val longitude = tempLon + 180
        val lonFirst = upper[(longitude / 20).toInt()]
        val lonSecond = ((longitude / 2) % 10).toInt().toString()
        val lonThird = lower[((longitude % 2) * 12).toInt()]

        return "$lonFirst$latFirst$lonSecond$latSecond$lonThird$latThird"
    }
}