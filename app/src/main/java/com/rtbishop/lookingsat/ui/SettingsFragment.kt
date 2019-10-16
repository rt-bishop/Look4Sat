package com.rtbishop.lookingsat.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.lookingsat.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }
}