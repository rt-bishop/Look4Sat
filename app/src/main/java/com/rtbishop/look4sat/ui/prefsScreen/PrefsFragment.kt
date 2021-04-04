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
package com.rtbishop.look4sat.ui.prefsScreen

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.QthConverter
import com.rtbishop.look4sat.utility.round
import com.rtbishop.look4sat.utility.showSnack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrefsFragment : PreferenceFragmentCompat() {
    
    @Inject
    lateinit var locationManager: LocationManager
    @Inject
    lateinit var prefsManager: PrefsManager
    @Inject
    lateinit var qthConverter: QthConverter
    
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) setPositionFromGPS()
            else requireView().showSnack(getString(R.string.pref_pos_gps_error))
        }
    
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
    
    private fun setPositionFromQth(qthString: String): Boolean {
        qthConverter.qthToLocation(qthString)?.let { gsp ->
            prefsManager.setStationPosition(gsp.latitude, gsp.longitude, gsp.heightAMSL)
            requireView().showSnack(getString(R.string.pref_pos_success))
            return true
        }
        requireView().showSnack(getString(R.string.pref_pos_qth_error))
        return false
    }
    
    private fun setPositionFromGPS() {
        val locPermString = Manifest.permission.ACCESS_FINE_LOCATION
        val locPermResult = ContextCompat.checkSelfPermission(requireContext(), locPermString)
        if (locPermResult == PackageManager.PERMISSION_GRANTED) {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                val latitude = location.latitude.round(4)
                val longitude = location.longitude.round(4)
                val altitude = location.altitude.round(1)
                prefsManager.setStationPosition(latitude, longitude, altitude)
                requireView().showSnack(getString(R.string.pref_pos_success))
            } else requireView().showSnack(getString(R.string.pref_pos_gps_null))
        } else requestPermissionLauncher.launch(locPermString)
    }
}
