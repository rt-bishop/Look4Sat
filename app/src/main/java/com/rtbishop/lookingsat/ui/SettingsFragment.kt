package com.rtbishop.lookingsat.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.lookingsat.MainViewModel
import com.rtbishop.lookingsat.R

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var viewModel: MainViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)

        val provider = EditTextPreference.SimpleSummaryProvider.getInstance()
        val latPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_lat))
        val lonPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_lon))
        val altPref = findPreference<EditTextPreference>(context!!.getText(R.string.key_alt))

        latPref?.summaryProvider = provider
        lonPref?.summaryProvider = provider
        altPref?.summaryProvider = provider
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == context!!.getText(R.string.key_lat) ||
            key == context!!.getText(R.string.key_lon) ||
            key == context!!.getText(R.string.key_alt)
        ) {
            viewModel.updateGsp()
        }
        if (key == context!!.getText(R.string.key_hours_ahead) ||
            key == context!!.getText(R.string.key_min_el)
        ) {
            viewModel.updatePassPref()
        }
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