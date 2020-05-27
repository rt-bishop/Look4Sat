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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentPolarViewBinding
import com.rtbishop.look4sat.ui.MainActivity
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.ui.adapters.TransmitterAdapter
import com.rtbishop.look4sat.ui.views.PolarView
import com.rtbishop.look4sat.utility.GeneralUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class PolarViewFragment : Fragment(R.layout.fragment_polar_view), SensorEventListener {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentPolarViewBinding
    private lateinit var satPass: SatPass
    private lateinit var sensorManager: SensorManager
    private var polarView: PolarView? = null
    private val args: PolarViewFragmentArgs by navArgs()
    private val transmitterAdapter = TransmitterAdapter()
    private var lastAccData = FloatArray(3)
    private var lastMagData = FloatArray(3)
    private var refreshJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        binding = FragmentPolarViewBinding.bind(view)
        (mainActivity.application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(mainActivity, modelFactory).get(SharedViewModel::class.java)
        sensorManager = mainActivity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        binding.recPolar.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = transmitterAdapter
        }
        observePasses()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.getCompass()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also { accData ->
                sensorManager.registerListener(this, accData, SensorManager.SENSOR_DELAY_GAME)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).also { magData ->
                sensorManager.registerListener(this, magData, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.getCompass()) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                filterSensorData(event.values, lastAccData)
            } else if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                filterSensorData(event.values, lastMagData)
            }

            val rotationMatrix = FloatArray(9)
            if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccData, lastMagData)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val degree = orientation[0] * 57.2957795f
                polarView?.rotation = -degree
            }
        }
    }

    private fun filterSensorData(input: FloatArray, output: FloatArray) {
        val filterStep = 0.05f
        input.indices.forEach { output[it] = output[it] + filterStep * (input[it] - output[it]) }
    }

    private fun observePasses() {
        viewModel.getPassList().observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Result.Success -> {
                    val refreshRate = viewModel.getRefreshRate()
                    satPass = result.data[args.satPassIndex]
                    mainActivity.supportActionBar?.title = satPass.tle.name
                    polarView = PolarView(mainActivity, satPass)
                    binding.framePolar.addView(polarView)
                    observeTransmitters()
                    refreshText(refreshRate)
                }
            }
        })
    }

    private fun observeTransmitters() {
        viewModel.getTransmittersForSat(satPass.tle.catnum).observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                transmitterAdapter.setList(it)
                transmitterAdapter.notifyDataSetChanged()
                binding.recPolar.visibility = View.VISIBLE
                binding.tvPolarNoTrans.visibility = View.INVISIBLE
            } else {
                binding.recPolar.visibility = View.INVISIBLE
                binding.tvPolarNoTrans.visibility = View.VISIBLE
            }
        })
    }

    private fun refreshText(rate: Long) {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            while (true) {
                setPassText()
                polarView?.invalidate()
                delay(rate)
            }
        }
    }

    private fun setPassText() {
        val satPos = satPass.predictor.getSatPos(Date())
        val polarAz = mainActivity.getString(R.string.pat_azimuth)
        val polarEl = mainActivity.getString(R.string.pat_elevation)
        val polarRng = mainActivity.getString(R.string.pat_distance)
        val polarAlt = mainActivity.getString(R.string.pat_altitude)
        binding.tvPolarAz.text = String.format(polarAz, GeneralUtils.rad2Deg(satPos.azimuth))
        binding.tvPolarEl.text = String.format(polarEl, GeneralUtils.rad2Deg(satPos.elevation))
        binding.tvPolarRng.text = String.format(polarRng, satPos.range)
        binding.tvPolarAlt.text = String.format(polarAlt, satPos.altitude)
    }
}
