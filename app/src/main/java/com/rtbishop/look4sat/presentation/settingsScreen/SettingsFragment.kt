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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentSettingsBinding
import com.rtbishop.look4sat.framework.PreferencesSource
import com.rtbishop.look4sat.presentation.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var preferences: PreferencesSource

    private val viewModel: SettingsViewModel by viewModels()
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
    private var _binding: FragmentSettingsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        _binding?.prefsBack?.setOnClickListener { findNavController().navigateUp() }
        setupAboutCard()
        setupDataCard()
        setupLocationCard()
        setupTrackingCard()
        setupOtherCard()
        setupWarrantyCard()
    }

    private fun setupAboutCard() {
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
            getNavResult<List<String>>(R.id.nav_prefs, "sources") { sources ->
                viewModel.updateDataFromWeb(sources)
            }
        }
    }

    private fun setupLocationCard() {
        _binding?.prefsLocation?.let { binding ->
            binding.locationBtnGps.setOnClickListener {
                updatePositionFromGPS()
            }
            binding.locationBtnQth.setOnClickListener {
                val editText = EditText(requireActivity())
                AlertDialog.Builder(requireContext())
                    .setTitle("Title")
                    .setEditText(editText)
                    .setPositiveButton("OK") { _, _ ->
                        val editTextInput = editText.text.toString()
                        updatePositionFromQth(editTextInput)
                    }
                    .setNeutralButton("Cancel", null)
                    .create()
                    .show()
            }
        }
    }

    private fun setupTrackingCard() {
        _binding?.prefsTracking?.let { binding ->
            binding.trackingSwitch.apply {
                isChecked = preferences.getRotatorEnabled()
                binding.trackingIp.isEnabled = isChecked
                binding.trackingIpEdit.setText(preferences.getRotatorIp())
                binding.trackingPort.isEnabled = isChecked
                binding.trackingPortEdit.setText(preferences.getRotatorPort())
                setOnCheckedChangeListener { _, isChecked ->
                    preferences.setRotatorEnabled(isChecked)
                    binding.trackingIp.isEnabled = isChecked
                    binding.trackingPort.isEnabled = isChecked
                }
            }
            binding.trackingIpEdit.doOnTextChanged { text, _, _, _ ->
                if (text.toString().isValidIPv4()) preferences.setRotatorIp(text.toString())
            }
            binding.trackingPortEdit.doOnTextChanged { text, _, _, _ ->
                if (text.toString().isValidPort()) preferences.setRotatorPort(text.toString())
            }
        }
    }

    private fun setupOtherCard() {
        _binding?.prefsOther?.let { binding ->
            binding.otherSwitchUtc.apply {
                isChecked = preferences.getUseUTC()
                setOnCheckedChangeListener { _, isChecked -> preferences.setUseUTC(isChecked) }
            }
            binding.otherSwitchSweep.apply {
                isChecked = preferences.getShowSweep()
                setOnCheckedChangeListener { _, isChecked -> preferences.setShowSweep(isChecked) }
            }
            binding.otherSwitchSensors.apply {
                isChecked = preferences.getUseCompass()
                setOnCheckedChangeListener { _, isChecked -> preferences.setUseCompass(isChecked) }
            }
        }
    }

    private fun setupWarrantyCard() {
        _binding?.prefsWarranty?.let { binding ->
            binding.warrantyThanks.movementMethod = LinkMovementMethod.getInstance()
            binding.warrantyLicense.movementMethod = LinkMovementMethod.getInstance()
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

    private fun updatePositionFromQth(qthString: String) {
        if (preferences.updatePositionFromQTH(qthString)) {
            showSnack(getString(R.string.pref_pos_success))
        } else {
            showSnack(getString(R.string.pref_pos_qth_error))
        }
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
