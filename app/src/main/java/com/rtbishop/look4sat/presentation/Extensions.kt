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
package com.rtbishop.look4sat.presentation

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).apply { show() }
}

fun SharedPreferences.getDouble(key: String, default: Double): Double {
    return Double.fromBits(getLong(key, default.toRawBits()))
}

fun SharedPreferences.Editor.putDouble(key: String, double: Double) {
    putLong(key, double.toRawBits())
}

fun <T> Fragment.getNavResult(@IdRes id: Int, key: String, onResult: (result: T) -> Unit) {
    val backStackEntry = findNavController().getBackStackEntry(id)
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME && backStackEntry.savedStateHandle.contains(key)) {
            backStackEntry.savedStateHandle.get<T>(key)?.let(onResult)
            backStackEntry.savedStateHandle.remove<T>(key)
        }
    }
    backStackEntry.lifecycle.addObserver(observer)
    viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) backStackEntry.lifecycle.removeObserver(observer)
    })
}

fun <T> Fragment.setNavResult(key: String, value: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, value)
}

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

fun Float.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun AlertDialog.Builder.setEditText(editText: EditText): AlertDialog.Builder {
    val container = FrameLayout(context)
    container.addView(editText)
    val containerParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
    val marginHorizontal = 32F
    val marginTop = 24F
    containerParams.topMargin = (marginTop / 2).toPx()
    containerParams.leftMargin = marginHorizontal.toInt()
    containerParams.rightMargin = marginHorizontal.toInt()
    container.layoutParams = containerParams
    val superContainer = FrameLayout(context)
    superContainer.addView(container)
    setView(superContainer)
    return this
}

fun ping(hostname: String, port: Int): Int {
    val start = System.currentTimeMillis()
    val socket = Socket()
    try {
        socket.connect(InetSocketAddress(hostname, port), 6000)
        socket.close()
    } catch (exception: Exception) {
        exception.printStackTrace()
        println("Failed to ping: $hostname")
        return Int.MAX_VALUE
    }
    return (System.currentTimeMillis() - start).toInt()
}
