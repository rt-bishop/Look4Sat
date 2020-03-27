/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.utility

import android.content.Context
import android.location.LocationManager
import android.util.Log
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.R
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.inject.Inject

class DataManager @Inject constructor(
    private val locationManager: LocationManager,
    private val context: Context
) {

    private val tag = "DataManager"
    private val tleMainListFileName = "tleFile.txt"
    private val tleSelectionFileName = "tleSelection"

    fun getLastKnownLocation(): GroundStationPosition? {
        val passiveProvider = LocationManager.PASSIVE_PROVIDER
        var groundStationPosition: GroundStationPosition? = null
        return try {
            val location = locationManager.getLastKnownLocation(passiveProvider)
            location?.let {
                groundStationPosition =
                    GroundStationPosition(it.latitude, it.longitude, it.altitude)
            }
            groundStationPosition
        } catch (e: SecurityException) {
            Log.w(tag, "No permissions")
            null
        }
    }

    fun saveSelectionList(selectionList: MutableList<Int>) {
        try {
            val fileOutputStream =
                context.openFileOutput(tleSelectionFileName, Context.MODE_PRIVATE)
            ObjectOutputStream(fileOutputStream).apply {
                writeObject(selectionList)
                flush()
                close()
            }
        } catch (exception: IOException) {
            Log.w(tag, "Can't save selection list")
        }
    }

    fun saveTleList(tleList: MutableList<TLE>) {
        try {
            val fileOutStream = context.openFileOutput(tleMainListFileName, Context.MODE_PRIVATE)
            ObjectOutputStream(fileOutStream).apply {
                writeObject(tleList)
                flush()
                close()
            }
        } catch (exception: IOException) {
            Log.w(tag, "Can't save TLE list")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun loadTleList(): List<TLE> {
        return try {
            val tleStream = context.openFileInput(tleMainListFileName)
            val tleList = ObjectInputStream(tleStream).readObject()
            tleList as List<TLE>
        } catch (exception: FileNotFoundException) {
            Log.w(tag, context.getString(R.string.err_no_tle_file))
            emptyList()
        } catch (exception: IOException) {
            Log.w(tag, exception.toString())
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun loadSelectionList(): MutableList<Int> {
        return try {
            val selectionStream = context.openFileInput(tleSelectionFileName)
            val selectionList = ObjectInputStream(selectionStream).readObject()
            selectionList as MutableList<Int>
        } catch (exception: FileNotFoundException) {
            Log.w(tag, context.getString(R.string.err_no_selection_file))
            mutableListOf()
        } catch (exception: IOException) {
            Log.w(tag, exception.toString())
            mutableListOf()
        }
    }
}
