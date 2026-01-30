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
package com.rtbishop.look4sat.domain.utility

import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.toTimerString(): String {
    val format = "%02d:%02d:%02d"
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return String.format(Locale.ENGLISH, format, hours, minutes, seconds)
}

fun Float.round(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

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

//fun String.isValidIPv4(): Boolean {
//    val ip4 = "^((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])(\\.(?!\$)|\$)){4}\$"
//    return this.matches(ip4.toRegex())
//}

//fun String.isValidPort(): Boolean {
//    val port = "([1-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])"
//    return this.matches(port.toRegex()) && this.toInt() in 1024..65535
//}
