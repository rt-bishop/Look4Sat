/*
 * LookingSat. Amateur radio & weather satellite tracker and passes calculator.
 * Copyright (C) 2019 Arty Bishop (bishop.arty@gmail.com)
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

package com.rtbishop.look4sat.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.look4sat.MainViewModel
import com.rtbishop.look4sat.R

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: MainViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(mainActivity).get(MainViewModel::class.java)

        val provider = EditTextPreference.SimpleSummaryProvider.getInstance()
        val latPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_lat))
        val lonPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_lon))
        val altPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_alt))
        val delayPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_delay))

        latPref?.summaryProvider = provider
        lonPref?.summaryProvider = provider
        altPref?.summaryProvider = provider
        delayPref?.summaryProvider = provider

        latPref?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        latPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isEmpty() || newValue.toString().toDouble() < -90.0 || newValue.toString().toDouble() > 90.0) {
                Toast.makeText(
                    mainActivity,
                    getString(R.string.pref_lat_input_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }

        lonPref?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        lonPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isEmpty() || newValue.toString().toDouble() < -180.0 || newValue.toString().toDouble() > 180.0) {
                Toast.makeText(
                    mainActivity,
                    getString(R.string.pref_lon_input_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }

        altPref?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        altPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isEmpty() || newValue.toString().toDouble() < -413.0 || newValue.toString().toDouble() > 8850.0) {
                Toast.makeText(
                    mainActivity,
                    getString(R.string.pref_alt_input_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }

        delayPref?.setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
        delayPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isEmpty() || newValue.toString().toLong() < 250 || newValue.toString().toLong() > 10000) {
                Toast.makeText(
                    mainActivity,
                    getString(R.string.pref_update_freq_input_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == context!!.getText(R.string.key_lat) ||
            key == context!!.getText(R.string.key_lon) ||
            key == context!!.getText(R.string.key_alt)
        ) viewModel.setGroundStationPosition()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}