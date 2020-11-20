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

package com.rtbishop.look4sat.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities
import com.rtbishop.look4sat.utility.Utilities.round
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrefsFragment : PreferenceFragmentCompat() {

    private val locPermReqCode = 1000
    private val locPermString = Manifest.permission.ACCESS_FINE_LOCATION

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var prefsManager: PrefsManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findPreference<Preference>(PrefsManager.keyPositionGPS)?.apply {
            setOnPreferenceClickListener {
                setPositionFromGPS()
                return@setOnPreferenceClickListener true
            }
        }

        findPreference<Preference>(PrefsManager.keyPositionQTH)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                setPositionFromQth(newValue.toString())
            }
        }
    }

    private fun setPositionFromQth(qthLocator: String): Boolean {
        val qthPattern = "[A-X][A-X][0-9][0-9][a-x][a-x]".toRegex()
        return if (qthLocator.matches(qthPattern)) {
            val location = Utilities.qthToGSP(qthLocator)
            val latitude = location.latitude.round(4)
            val longitude = location.longitude.round(4)
            prefsManager.setStationPosition(latitude, longitude, location.heightAMSL)
            showSnack(getString(R.string.pref_pos_success))
            true
        } else {
            showSnack(getString(R.string.pref_pos_qth_error))
            false
        }
    }

    private fun setPositionFromGPS() {
        val locPermResult = ContextCompat.checkSelfPermission(requireContext(), locPermString)
        if (locPermResult == PackageManager.PERMISSION_GRANTED) {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                val latitude = location.latitude.round(4)
                val longitude = location.longitude.round(4)
                val altitude = location.altitude.round(1)
                prefsManager.setStationPosition(latitude, longitude, altitude)
                showSnack(getString(R.string.pref_pos_success))
            } else showSnack(getString(R.string.pref_pos_gps_null))
        } else requestPermissions(arrayOf(locPermString), locPermReqCode)
    }

    private fun showSnack(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
            .setAnchorView(R.id.nav_bottom)
            .show()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, result: IntArray) {
        if (code == locPermReqCode) {
            if (result.isNotEmpty() && result[0] == PackageManager.PERMISSION_GRANTED) {
                setPositionFromGPS()
            } else showSnack(getString(R.string.pref_pos_gps_error))
        }
    }
}
