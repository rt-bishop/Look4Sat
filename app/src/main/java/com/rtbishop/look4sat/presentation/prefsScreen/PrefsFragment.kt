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
package com.rtbishop.look4sat.presentation.prefsScreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentPrefsBinding
import com.rtbishop.look4sat.framework.PreferencesSource
import com.rtbishop.look4sat.presentation.getNavResult
import com.rtbishop.look4sat.presentation.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrefsFragment : Fragment(R.layout.fragment_prefs) {

    @Inject
    lateinit var preferences: PreferencesSource
    private val viewModel: PrefsViewModel by viewModels()
    private val locPermFine = Manifest.permission.ACCESS_FINE_LOCATION
    private val locPermCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val contentContract = ActivityResultContracts.GetContent()
    private val filePicker = registerForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.updateDataFromFile(uri) }
    }
    private val permReqContract = ActivityResultContracts.RequestMultiplePermissions()
    private val locPermReq = registerForActivityResult(permReqContract) { permissions ->
        when {
            permissions[locPermFine] == true -> updatePositionFromGPS()
            permissions[locPermCoarse] == true -> updatePositionFromGPS()
            else -> showSnack(getString(R.string.pref_pos_gps_error))
        }
    }
    private var _binding: FragmentPrefsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPrefsBinding.bind(view)
        setupInfoCard()
        setupDataCard()
        setupLocationCard()
        setupWarrantyCard()
    }

    private fun setupInfoCard() {
        _binding?.prefsInfo?.let { binding ->
            binding.aboutVersion.text =
                String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME)
            binding.aboutBtnGithub.setOnClickListener {
                gotoUrl("https://github.com/rt-bishop/Look4Sat/")
            }
            binding.aboutBtnDonate.setOnClickListener {
                gotoUrl("https://www.buymeacoffee.com/rtbishop")
            }
            binding.aboutBtnFdroid.setOnClickListener {
                gotoUrl("https://f-droid.org/en/packages/com.rtbishop.look4sat/")
            }
        }
    }

    private fun setupDataCard() {
        _binding?.prefsData?.let { binding ->
            binding.updateBtnWeb.setOnClickListener {
                findNavController().navigateSafe(R.id.action_prefs_to_sources)
            }
            binding.updateBtnFile.setOnClickListener { filePicker.launch("*/*") }
        }
        getNavResult<List<String>>(R.id.nav_passes, "sources") { sources ->
            viewModel.updateDataFromWeb(sources)
        }
    }

    private fun setupLocationCard() {

    }

    private fun setupWarrantyCard() {
        _binding?.prefsWarranty?.let { binding ->
            val moveMethod = LinkMovementMethod.getInstance()
            binding.warrantyThanks.movementMethod = moveMethod
            binding.warrantyLicense.movementMethod = moveMethod
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

    private fun updatePositionFromQth(qthString: String): Boolean {
        return if (preferences.updatePositionFromQTH(qthString)) {
            showSnack(getString(R.string.pref_pos_success))
            true
        } else {
            showSnack(getString(R.string.pref_pos_qth_error))
            false
        }
    }

    private fun setupLater() {
//        findPreference<Preference>(PreferencesSource.keyPositionGPS)?.apply {
//            setOnPreferenceClickListener {
//                updatePositionFromGPS()
//                return@setOnPreferenceClickListener true
//            }
//        }
//
//        findPreference<Preference>(PreferencesSource.keyPositionQTH)?.apply {
//            setOnPreferenceChangeListener { _, newValue ->
//                updatePositionFromQth(newValue.toString())
//            }
//        }
//
//        findPreference<EditTextPreference>(PreferencesSource.keyRotatorAddress)?.apply {
//            setOnPreferenceChangeListener { _, newValue ->
//                val ip4 = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!\$)|\$)){4}\$"
//                if (newValue.toString().matches(ip4.toRegex())) {
//                    return@setOnPreferenceChangeListener true
//                } else {
//                    showSnack(getString(R.string.tracking_ip_invalid))
//                    return@setOnPreferenceChangeListener false
//                }
//            }
//        }
//
//        findPreference<EditTextPreference>(PreferencesSource.keyRotatorPort)?.apply {
//            setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
//            setOnPreferenceChangeListener { _, newValue ->
//                val portValue = newValue.toString()
//                if (portValue.isNotEmpty() && portValue.toInt() in 1024..65535) {
//                    return@setOnPreferenceChangeListener true
//                } else {
//                    showSnack(getString(R.string.tracking_port_invalid))
//                    return@setOnPreferenceChangeListener false
//                }
//            }
//        }
    }

    private fun showSnack(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun gotoUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
