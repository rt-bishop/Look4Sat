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

import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities.round
import com.rtbishop.look4sat.utility.Utilities.snack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrefsFragment : PreferenceFragmentCompat() {

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
    }

    private fun setPositionFromGPS() {
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                val latitude = location.latitude.round(4)
                val longitude = location.longitude.round(4)
                val altitude = location.altitude.round(1)
                prefsManager.setStationPosition(latitude, longitude, altitude)
                getString(R.string.pref_pos_gps_success).snack(requireView())
            } else {
                getString(R.string.pref_pos_gps_null).snack(requireView())
            }
        } catch (e: SecurityException) {
            getString(R.string.pref_pos_gps_error).snack(requireView())
        }
    }
}
