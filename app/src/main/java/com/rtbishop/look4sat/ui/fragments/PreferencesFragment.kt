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

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.ui.MainActivity
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.utility.GeneralUtils.toast

class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var keyLat: String
    private lateinit var keyLon: String
    private lateinit var keyAlt: String
    private lateinit var keyDelay: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        val provider = EditTextPreference.SimpleSummaryProvider.getInstance()

        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)

        keyLat = mainActivity.getString(R.string.pref_lat_key)
        keyLon = mainActivity.getString(R.string.pref_lon_key)
        keyAlt = mainActivity.getString(R.string.pref_alt_key)
        keyDelay = mainActivity.getString(R.string.pref_refresh_rate_key)

        findPreference<EditTextPreference>(keyLat)?.apply {
            summaryProvider = provider
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr == "-" || valueStr.toDouble() < -90.0 || valueStr.toDouble() > 90.0) {
                    getString(R.string.pref_lat_input_error).toast(mainActivity)
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference<EditTextPreference>(keyLon)?.apply {
            summaryProvider = provider
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr == "-" || valueStr.toDouble() < -180.0 || valueStr.toDouble() > 180.0) {
                    getString(R.string.pref_lon_input_error).toast(mainActivity)
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference<EditTextPreference>(keyAlt)?.apply {
            summaryProvider = provider
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr == "-" || valueStr.toDouble() < -413.0 || valueStr.toDouble() > 8850.0) {
                    getString(R.string.pref_alt_input_error).toast(mainActivity)
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference<EditTextPreference>(keyDelay)?.apply {
            summaryProvider = provider
            setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr.toLong() < 250 || valueStr.toLong() > 10000) {
                    getString(R.string.pref_refresh_rate_input_error).toast(mainActivity)
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == keyLat || key == keyLon || key == keyAlt) viewModel.setPositionFromPref()
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
