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
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentSettingsBinding
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.presentation.getNavResult
import com.rtbishop.look4sat.presentation.isValidIPv4
import com.rtbishop.look4sat.presentation.isValidPort
import com.rtbishop.look4sat.presentation.setEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private val locationFine = Manifest.permission.ACCESS_FINE_LOCATION
    private val locationCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val locationContract = ActivityResultContracts.RequestMultiplePermissions()
    private val locationRequest = registerForActivityResult(locationContract) { permissions ->
        when {
            permissions[locationFine] == true -> viewModel.setPositionFromGps()
            permissions[locationCoarse] == true -> viewModel.setPositionFromNet()
            else -> showToast(getString(R.string.pref_pos_gps_error))
        }
    }
    private val contentContract = ActivityResultContracts.GetContent()
    private val contentRequest = registerForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.updateDataFromFile(uri.toString()) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)
        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        binding.settingsScroll.apply {
            setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, y, _, newY ->
                if (y > newY) binding.settingsFab.hide() else binding.settingsFab.show()
            })
        }
        binding.settingsBack.setOnClickListener { findNavController().navigateUp() }
        setupAboutCard()
        setupDataCard()
        setupLocationCard()
        setupTrackingCard()
        setupOtherCard()
        setupWarrantyCard()
    }

    private fun setupObservers() {
        viewModel.stationPosition.asLiveData().observe(viewLifecycleOwner) { stationPos ->
            stationPos?.let { handleStationPosition(stationPos) }
        }
        viewModel.getUpdateState().asLiveData().observe(viewLifecycleOwner) { updateState ->
            updateState?.let { handleSatState(updateState) }
        }
    }

    private fun setupAboutCard() {
        binding.settingsInfo.aboutVersion.text =
            String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME)
        binding.settingsInfo.aboutBtnGithub.setOnClickListener {
            gotoUrl("https://github.com/rt-bishop/Look4Sat/")
        }
        binding.settingsInfo.aboutBtnDonate.setOnClickListener {
            gotoUrl("https://www.buymeacoffee.com/rtbishop")
        }
        binding.settingsInfo.aboutBtnFdroid.setOnClickListener {
            gotoUrl("https://f-droid.org/en/packages/com.rtbishop.look4sat/")
        }
    }

    private fun setupLocationCard() {
        setPositionText(viewModel.getStationPosition())
        binding.settingsLocation.locationBtnGps.setOnClickListener {
            locationRequest.launch(arrayOf(locationFine, locationCoarse))
        }
        binding.settingsLocation.locationBtnQth.setOnClickListener {
            val editText = EditText(requireActivity())
            AlertDialog.Builder(requireContext())
                .setTitle("Title")
                .setEditText(editText)
                .setPositiveButton("OK") { _, _ ->
                    val editTextInput = editText.text.toString()
                    viewModel.setPositionFromQth(editTextInput)
                }
                .setNeutralButton("Cancel", null)
                .create()
                .show()
        }
    }

    private fun setupDataCard() {
        binding.settingsData.updateBtnFile.setOnClickListener { contentRequest.launch("*/*") }
        binding.settingsData.updateBtnWeb.setOnClickListener {
            val action = SettingsFragmentDirections.actionPrefsToSources()
            findNavController().navigate(action)
        }
        binding.settingsData.updateBtnClear.setOnClickListener { viewModel.clearData() }
        getNavResult<List<String>>(R.id.nav_settings, "sources") { sources ->
            viewModel.updateDataFromWeb(sources)
        }
    }

    private fun setupTrackingCard() {
        binding.settingsTracking.trackingSwitch.apply {
            isChecked = viewModel.getRotatorEnabled()
            binding.settingsTracking.trackingIp.isEnabled = isChecked
            binding.settingsTracking.trackingIpEdit.setText(viewModel.getRotatorServer())
            binding.settingsTracking.trackingPort.isEnabled = isChecked
            binding.settingsTracking.trackingPortEdit.setText(viewModel.getRotatorPort())
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setRotatorEnabled(isChecked)
                binding.settingsTracking.trackingIp.isEnabled = isChecked
                binding.settingsTracking.trackingPort.isEnabled = isChecked
            }
        }
        binding.settingsTracking.trackingIpEdit.doOnTextChanged { text, _, _, _ ->
            if (text.toString().isValidIPv4()) viewModel.setRotatorServer(text.toString())
        }
        binding.settingsTracking.trackingPortEdit.doOnTextChanged { text, _, _, _ ->
            if (text.toString().isValidPort()) viewModel.setRotatorPort(text.toString())
        }
    }

    private fun setupOtherCard() {
        binding.settingsOther.otherSwitchUtc.apply {
            isChecked = viewModel.getUseUTC()
            setOnCheckedChangeListener { _, isChecked -> viewModel.setUseUTC(isChecked) }
        }
        binding.settingsOther.otherSwitchSweep.apply {
            isChecked = viewModel.getShowSweep()
            setOnCheckedChangeListener { _, isChecked -> viewModel.setShowSweep(isChecked) }
        }
        binding.settingsOther.otherSwitchSensors.apply {
            isChecked = viewModel.getUseCompass()
            setOnCheckedChangeListener { _, isChecked -> viewModel.setUseCompass(isChecked) }
        }
    }

    private fun setupWarrantyCard() {
        binding.settingsWarranty.warrantyThanks.movementMethod = LinkMovementMethod.getInstance()
        binding.settingsWarranty.warrantyLicense.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setPositionText(geoPos: GeoPos) {
        val latFormat = getString(R.string.location_lat)
        val lonFormat = getString(R.string.location_lon)
        binding.settingsLocation.locationLat.text = String.format(latFormat, geoPos.latitude)
        binding.settingsLocation.locationLon.text = String.format(lonFormat, geoPos.longitude)
    }

    private fun handleStationPosition(pos: DataState<GeoPos>) {
        when (pos) {
            is DataState.Success -> {
                setPositionText(pos.data)
                binding.settingsLocation.locationProgress.isIndeterminate = false
                viewModel.setPositionHandled()
                showToast(getString(R.string.pref_pos_success))
            }
            is DataState.Error -> {
                binding.settingsLocation.locationProgress.isIndeterminate = false
                viewModel.setPositionHandled()
                showToast(pos.message.toString())
            }
            DataState.Loading -> {
                binding.settingsLocation.locationProgress.isIndeterminate = true
            }
            DataState.Handled -> {}
        }
    }

    private fun handleSatState(state: DataState<Long>) {
        when (state) {
            is DataState.Success -> {
                binding.settingsData.updateProgress.isIndeterminate = false
                viewModel.setUpdateHandled()
                showToast("Data updated successfully")
            }
            is DataState.Error -> {
                binding.settingsData.updateProgress.isIndeterminate = false
                viewModel.setUpdateHandled()
                showToast(state.message.toString())
            }
            is DataState.Loading -> {
                binding.settingsData.updateProgress.isIndeterminate = true
            }
            is DataState.Handled -> {}
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun gotoUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
