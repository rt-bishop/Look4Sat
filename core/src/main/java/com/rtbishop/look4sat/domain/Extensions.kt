package com.rtbishop.look4sat.domain

import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

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

fun String.getHash(type: String = "SHA-256"): String {
    val hexChars = "0123456789ABCDEF"
    val bytes = MessageDigest.getInstance(type).digest(this.toByteArray())
    val result = StringBuilder(bytes.size * 2)
    bytes.forEach {
        val i = it.toInt()
        result.append(hexChars[i shr 4 and 0x0f])
        result.append(hexChars[i and 0x0f])
    }
    return result.toString()
}

fun String.isValidEmail(): Boolean {
    val expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,8}$"
    val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
    return pattern.matcher(this).matches()
}

fun String.isValidIPv4(): Boolean {
    val ip4 = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!\$)|\$)){4}\$"
    return this.matches(ip4.toRegex())
}

fun String.isValidPort(): Boolean {
    return this.isNotEmpty() && this.toInt() in 1024..65535
}

fun ping(hostname: String, port: Int): Int {
    val start = System.currentTimeMillis()
    val socket = Socket()
    try {
        socket.connect(InetSocketAddress(hostname, port), 5000)
        socket.close()
    } catch (exception: Exception) {
        exception.printStackTrace()
        println("Failed to ping: $hostname")
        return Int.MAX_VALUE
    }
    return (System.currentTimeMillis() - start).toInt()
}
