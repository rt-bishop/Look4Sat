/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.rtbishop.look4sat.presentation.clickWithDebounce
import com.rtbishop.look4sat.presentation.getNavResult
import com.rtbishop.look4sat.utility.isValidIPv4
import com.rtbishop.look4sat.utility.isValidPort
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()
    private val bluetooth = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }
    private val bluetoothContract = ActivityResultContracts.RequestPermission()
    private val bluetoothRequest = registerForActivityResult(bluetoothContract) { isGranted ->
        if (!isGranted) {
            showToast(getString(R.string.BTremote_perm_error))
            toggleBTstate(isGranted)
        }
    }
    private val locationFine = Manifest.permission.ACCESS_FINE_LOCATION
    private val locationCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val locationContract = ActivityResultContracts.RequestMultiplePermissions()
    private val locationRequest = registerForActivityResult(locationContract) { permissions ->
        when {
            permissions[locationFine] == true -> viewModel.setPositionFromGps()
            permissions[locationCoarse] == true -> viewModel.setPositionFromNet()
            else -> showToast(getString(R.string.location_gps_error))
        }
    }
    private val contentContract = ActivityResultContracts.GetContent()
    private val contentRequest = registerForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.updateFromFile(uri.toString()) }
    }
    private lateinit var binding: FragmentSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view).apply {
            settingsBtnBack.clickWithDebounce { findNavController().navigateUp() }
            settingsScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, y, _, newY ->
                if (y > newY) settingsFab.hide() else settingsFab.show()
            })
            settingsAbout.aboutVersion.text =
                String.format(getString(R.string.app_version), BuildConfig.VERSION_NAME)
            settingsBtnGithub.clickWithDebounce {
                gotoUrl("https://github.com/rt-bishop/Look4Sat/")
            }
            settingsFab.clickWithDebounce {
                gotoUrl("https://ko-fi.com/rt_bishop")
            }
            settingsBtnFdroid.clickWithDebounce {
                gotoUrl("https://f-droid.org/en/packages/com.rtbishop.look4sat/")
            }
        }
        setupLocationCard()
        setupDataCard()
        setupRemoteCard()
        setupBTCard()
        setupOtherCard()
        setupOutroCard()
        viewModel.stationPosition.asLiveData().observe(viewLifecycleOwner) { stationPos ->
            stationPos?.let { handleStationPosition(stationPos) }
        }
        viewModel.getUpdateState().asLiveData().observe(viewLifecycleOwner) { updateState ->
            updateState?.let { handleSatState(updateState) }
        }
    }

    private fun setupLocationCard() {
        binding.run {
            setPositionText(viewModel.getStationPosition())
            settingsLocation.locationBtnGps.clickWithDebounce {
                locationRequest.launch(arrayOf(locationFine, locationCoarse))
            }
            settingsLocation.locationBtnManual.clickWithDebounce {
                val action = SettingsFragmentDirections.globalToPosition()
                findNavController().navigate(action)
            }
            settingsLocation.locationBtnQth.clickWithDebounce {
                val action = SettingsFragmentDirections.globalToLocator()
                findNavController().navigate(action)
            }
            getNavResult<Pair<Double, Double>>(R.id.nav_settings, "position") { position ->
                viewModel.setStationPosition(position.first, position.second)
            }
            getNavResult<String>(R.id.nav_settings, "locator") { locator ->
                viewModel.setPositionFromQth(locator)
            }
        }
    }

    private fun setupDataCard() {
        binding.run {
            setUpdateTime(viewModel.getLastUpdateTime())
            settingsData.dataBtnWeb.clickWithDebounce { viewModel.updateFromWeb() }
            settingsData.dataBtnFile.clickWithDebounce { contentRequest.launch("*/*") }
            settingsData.dataBtnClear.clickWithDebounce { viewModel.clearAllData() }
            viewModel.entriesTotal.observe(viewLifecycleOwner) { number ->
                val entriesFormat = getString(R.string.data_entries)
                settingsData.dataEntries.text = String.format(entriesFormat, number)
            }
            viewModel.radiosTotal.observe(viewLifecycleOwner) { number ->
                val radiosFormat = getString(R.string.data_radios)
                settingsData.dataRadios.text = String.format(radiosFormat, number)
            }
            getNavResult<List<String>>(R.id.nav_settings, "sources") {
                viewModel.updateFromWeb()
            }
        }
    }

    private fun setupRemoteCard() {
        binding.run {
            settingsRemote.remoteSwitch.apply {
                isChecked = viewModel.getRotatorEnabled()
                settingsRemote.remoteIp.isEnabled = isChecked
                settingsRemote.remoteIpEdit.setText(viewModel.getRotatorServer())
                settingsRemote.remotePort.isEnabled = isChecked
                settingsRemote.remotePortEdit.setText(viewModel.getRotatorPort())
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.setRotatorEnabled(isChecked)
                    settingsRemote.remoteIp.isEnabled = isChecked
                    settingsRemote.remotePort.isEnabled = isChecked
                }
            }
            settingsRemote.remoteIpEdit.doOnTextChanged { text, _, _, _ ->
                if (text.toString().isValidIPv4()) viewModel.setRotatorServer(text.toString())
            }
            settingsRemote.remotePortEdit.doOnTextChanged { text, _, _, _ ->
                if (text.toString().isValidPort()) viewModel.setRotatorPort(text.toString())
            }
        }
    }

    private fun setupBTCard() {
        binding.run {
            settingsBtremote.BTremoteSwitch.apply {
                isChecked = viewModel.getBTEnabled()
                settingsBtremote.BTremoteAddress.isEnabled = isChecked
                settingsBtremote.BTAddressEdit.setText(viewModel.getBTDeviceAddr())
                settingsBtremote.BTremoteFormat.isEnabled = isChecked
                settingsBtremote.BTFormatEdit.setText(viewModel.getBTFormat())
                setOnCheckedChangeListener { _, isChecked ->
                    toggleBTstate(isChecked)
                    bluetoothRequest.launch(bluetooth)
                }
            }
            settingsBtremote.BTAddressEdit.doOnTextChanged { text, _, _, _ ->
                viewModel.setBTDeviceAddr(text.toString())
            }
            settingsBtremote.BTFormatEdit.doOnTextChanged { text, _, _, _ ->
                viewModel.setBTFormat(text.toString())
            }
        }
    }

    private fun toggleBTstate(value: Boolean) {
        binding.run {
            viewModel.setBTEnabled(value)
            settingsBtremote.BTremoteSwitch.isChecked = value
            settingsBtremote.BTremoteAddress.isEnabled = value
            settingsBtremote.BTremoteFormat.isEnabled = value
        }
    }

    private fun setupOtherCard() {
        binding.run {
            settingsOther.otherSwitchUtc.apply {
                isChecked = viewModel.getUseUTC()
                setOnCheckedChangeListener { _, isChecked -> viewModel.setUseUTC(isChecked) }
            }
            settingsOther.otherSwitchUpdate.apply {
                isChecked = viewModel.getAutoUpdateEnabled()
                setOnCheckedChangeListener { _, isChecked -> viewModel.setAutoUpdateEnabled(isChecked) }
            }
            settingsOther.otherSwitchSweep.apply {
                isChecked = viewModel.getShowSweep()
                setOnCheckedChangeListener { _, isChecked -> viewModel.setShowSweep(isChecked) }
            }
            settingsOther.otherSwitchSensors.apply {
                isChecked = viewModel.getUseCompass()
                setOnCheckedChangeListener { _, isChecked -> viewModel.setUseCompass(isChecked) }
            }
        }
    }

    private fun setupOutroCard() {
        binding.run {
            settingsOutro.outroThanks.movementMethod = LinkMovementMethod.getInstance()
            settingsOutro.outroLicense.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun handleStationPosition(pos: DataState<GeoPos>) {
        when (pos) {
            is DataState.Success -> {
                setPositionText(pos.data)
                binding.settingsLocation.locationProgress.isIndeterminate = false
                viewModel.setPositionHandled()
                showToast(getString(R.string.location_success))
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

    private fun setPositionText(geoPos: GeoPos) {
        binding.run {
            val latFormat = getString(R.string.location_lat)
            val lonFormat = getString(R.string.location_lon)
            settingsLocation.locationLat.text = String.format(latFormat, geoPos.lat)
            settingsLocation.locationLon.text = String.format(lonFormat, geoPos.lon)
        }
    }

    private fun handleSatState(state: DataState<Long>) {
        when (state) {
            is DataState.Success -> {
                binding.settingsData.dataProgress.isIndeterminate = false
                setUpdateTime(state.data)
                viewModel.setUpdateHandled()
                if (state.data == 0L) {
                    showToast(getString(R.string.data_clear_success))
                } else {
                    showToast(getString(R.string.data_success))
                }
            }
            is DataState.Error -> {
                binding.settingsData.dataProgress.isIndeterminate = false
                viewModel.setUpdateHandled()
                showToast(getString(R.string.data_error))
            }
            is DataState.Loading -> {
                binding.settingsData.dataProgress.isIndeterminate = true
            }
            is DataState.Handled -> {}
        }
    }

    private fun setUpdateTime(updateTime: Long) {
        val updatePattern = getString(R.string.data_update)
        val updateDate = if (updateTime == 0L) {
            getString(R.string.pass_placeholder)
        } else {
            SimpleDateFormat("d MMM yyyy - HH:mm:ss", Locale.getDefault()).format(Date(updateTime))
        }
        binding.settingsData.dataUpdate.text = String.format(updatePattern, updateDate)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun gotoUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
