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
package com.rtbishop.look4sat.utility

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

fun View.showSnack(message: String, isAnchored: Boolean = true) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).apply {
        if (isAnchored) setAnchorView(R.id.main_nav_bottom)
    }.show()
}

fun NavController.navigateSafe(
    @IdRes resId: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null,
    navExtras: Navigator.Extras? = null
) {
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)
    if (action != null && currentDestination?.id != action.destinationId) {
        navigate(resId, args, navOptions, navExtras)
    }
}

fun <T> Fragment.setNavResult(key: String, value: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, value)
}

fun <T> Fragment.getNavResult(@IdRes id: Int, key: String, onResult: (result: T) -> Unit) {
    val navBackStackEntry = findNavController().getBackStackEntry(id)
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME
            && navBackStackEntry.savedStateHandle.contains(key)
        ) {
            val result = navBackStackEntry.savedStateHandle.get<T>(key)
            result?.let(onResult)
            navBackStackEntry.savedStateHandle.remove<T>(key)
        }
    }
    navBackStackEntry.lifecycle.addObserver(observer)
    viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            navBackStackEntry.lifecycle.removeObserver(observer)
        }
    })
}

fun Int.toDate(): Date = Date(this.toLong() * 1000L)

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

fun Date.toString(format: String): String {
    val dateFormatter = SimpleDateFormat(format, Locale.US)
    return dateFormatter.format(this)
}

fun String.toDate(format: String): Date? {
    val dateFormatter = SimpleDateFormat(format, Locale.US)
    return try {
        dateFormatter.parse(this)
    } catch (e: ParseException) {
        null
    }
}

fun String.toUri(): Uri? {
    return try {
        if (URLUtil.isValidUrl(this))
            Uri.parse(this)
        else
            null
    } catch (e: Exception) {
        null
    }
}

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.sha1(): String {
    val bytes = MessageDigest.getInstance("SHA-1").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.isEmailValid(): Boolean {
    val expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,8}$"
    val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(this)
    return matcher.matches()
}

fun String.getJsonObjectOrNull(): JSONObject? {
    return try {
        JSONObject(this)
    } catch (e: JSONException) {
        null
    }
}

fun String.getJsonArrayOrNull(): JSONArray? {
    return try {
        JSONArray(this)
    } catch (e: JSONException) {
        null
    }
}

fun JSONObject.getIntOrNull(name: String): Int? =
    try {
        getInt(name)
    } catch (e: JSONException) {
        val strValue = getStringOrNull(name)
        strValue?.toIntOrNull()
    }

fun JSONObject.getDoubleOrNull(name: String): Double? =
    try {
        getDouble(name)
    } catch (e: JSONException) {
        null
    }

fun JSONObject.getLongOrNull(name: String): Long? =
    try {
        getLong(name)
    } catch (e: JSONException) {
        null
    }

fun JSONObject.getStringOrNull(name: String): String? =
    try {
        getString(name).trim()
    } catch (e: JSONException) {
        null
    }

fun JSONObject.getBooleanOrNull(name: String): Boolean? =
    try {
        getBoolean(name)
    } catch (e: JSONException) {
        null
    }

fun JSONObject.getObjectOrNull(name: String): JSONObject? =
    try {
        getJSONObject(name)
    } catch (e: JSONException) {
        null
    }

fun JSONObject.getArrayOrNull(name: String): JSONArray? =
    try {
        getJSONArray(name)
    } catch (e: JSONException) {
        null
    }

fun JSONObject.getArrayOrEmpty(name: String): JSONArray =
    try {
        getJSONArray(name)
    } catch (e: JSONException) {
        JSONArray()
    }
