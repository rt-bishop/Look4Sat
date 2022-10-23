/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.utility

import com.rtbishop.look4sat.domain.predict.DEG2RAD
import com.rtbishop.look4sat.domain.predict.RAD2DEG
import java.util.concurrent.TimeUnit

fun Long.toTimerString(): String {
    val format = "%02d:%02d:%02d"
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return String.format(format, hours, minutes, seconds)
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

fun Double.toDegrees(): Double = this * RAD2DEG

fun Double.toRadians(): Double = this * DEG2RAD

//fun String.getHash(type: String = "SHA-256"): String {
//    val hexChars = "0123456789ABCDEF"
//    val bytes = MessageDigest.getInstance(type).digest(this.toByteArray())
//    val result = StringBuilder(bytes.size * 2)
//    bytes.forEach {
//        val i = it.toInt()
//        result.append(hexChars[i shr 4 and 0x0f])
//        result.append(hexChars[i and 0x0f])
//    }
//    return result.toString()
//}

//fun String.isValidEmail(): Boolean {
//    val expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,8}$"
//    val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
//    return pattern.matcher(this).matches()
//}

fun String.isValidIPv4(): Boolean {
    val ip4 = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!\$)|\$)){4}\$"
    return this.matches(ip4.toRegex())
}

fun String.isValidPort(): Boolean {
    val port = "([1-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])"
    return this.matches(port.toRegex()) && this.toInt() in 1024..65535
}

//fun ping(hostname: String, port: Int): Int {
//    val start = System.currentTimeMillis()
//    val socket = Socket()
//    try {
//        socket.connect(InetSocketAddress(hostname, port), 5000)
//        socket.close()
//    } catch (exception: Exception) {
//        exception.printStackTrace()
//        println("Failed to ping: $hostname")
//        return Int.MAX_VALUE
//    }
//    return (System.currentTimeMillis() - start).toInt()
//}
