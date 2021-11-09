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
package com.rtbishop.look4sat.presentation.settingsScreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.framework.PreferencesSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var preferences: PreferencesSource

    private val permReqContract = ActivityResultContracts.RequestMultiplePermissions()
    private val locPermFine = Manifest.permission.ACCESS_FINE_LOCATION
    private val locPermCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val locPermReq = registerForActivityResult(permReqContract) { permissions ->
        when {
            permissions[locPermFine] == true -> updatePositionFromGPS()
            permissions[locPermCoarse] == true -> updatePositionFromGPS()
            else -> showSnack(getString(R.string.pref_pos_gps_error))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findPreference<Preference>(PreferencesSource.keyPositionGPS)?.apply {
            setOnPreferenceClickListener {
                updatePositionFromGPS()
                return@setOnPreferenceClickListener true
            }
        }

        findPreference<Preference>(PreferencesSource.keyPositionQTH)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updatePositionFromQth(newValue.toString())
            }
        }

        findPreference<EditTextPreference>(PreferencesSource.keyRotatorAddress)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val ip4 = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!\$)|\$)){4}\$"
                if (newValue.toString().matches(ip4.toRegex())) {
                    return@setOnPreferenceChangeListener true
                } else {
                    showSnack(getString(R.string.tracking_rotator_address_invalid))
                    return@setOnPreferenceChangeListener false
                }
            }
        }

        findPreference<EditTextPreference>(PreferencesSource.keyRotatorPort)?.apply {
            setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
            setOnPreferenceChangeListener { _, newValue ->
                val portValue = newValue.toString()
                if (portValue.isNotEmpty() && portValue.toInt() in 1024..65535) {
                    return@setOnPreferenceChangeListener true
                } else {
                    showSnack(getString(R.string.tracking_rotator_port_invalid))
                    return@setOnPreferenceChangeListener false
                }
            }
        }
    }

    private fun updatePositionFromQth(qthString: String): Boolean {
        return if (preferences.updatePositionFromQTH(qthString)) {
            showSnack(getString(R.string.pref_pos_success))
            true
        } else {
            showSnack(getString(R.string.pref_pos_qth_error))
            false
        }
    }

    private fun updatePositionFromGPS() {
        val locPermString = Manifest.permission.ACCESS_FINE_LOCATION
        val locPermResult = ContextCompat.checkSelfPermission(requireContext(), locPermString)
        if (locPermResult == PackageManager.PERMISSION_GRANTED) {
            if (preferences.updatePositionFromGPS()) {
                showSnack(getString(R.string.pref_pos_success))
            } else {
                showSnack(getString(R.string.pref_pos_gps_null))
            }
        } else locPermReq.launch(arrayOf(locPermFine, locPermCoarse))
    }

    private fun showSnack(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
}
