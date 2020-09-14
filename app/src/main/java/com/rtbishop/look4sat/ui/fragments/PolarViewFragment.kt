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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.round

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
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
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
        event?.let { useSoftwareSensor(it) }
    }

    private fun useSoftwareSensor(event: SensorEvent) {
        val rotationValues = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationValues, event.values)
        val orientationValues = FloatArray(3)
        SensorManager.getOrientation(rotationValues, orientationValues)
        val magneticAzimuth = ((orientationValues[0] * 57.2957795f) + 360f) % 360f
        val roundedAzimuth = round(magneticAzimuth * 100) / 100
        polarView?.rotation = -roundedAzimuth
    }

    private fun filterSensorData(input: FloatArray, output: FloatArray) {
        val filterStep = 1f
        input.indices.forEach { output[it] = output[it] + filterStep * (input[it] - output[it]) }
    }

    private fun observePasses() {
        viewModel.getPassList().observe(viewLifecycleOwner, { result ->
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
        viewModel.getTransmittersForSat(satPass.tle.catnum).observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                transmitterAdapter.setPredictor(satPass.predictor)
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
        lifecycleScope.launch {
            while (true) {
                setPassText()
                polarView?.invalidate()
                transmitterAdapter.notifyDataSetChanged()
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
        binding.tvPolarAz.text = String.format(polarAz, Math.toDegrees(satPos.azimuth))
        binding.tvPolarEl.text = String.format(polarEl, Math.toDegrees(satPos.elevation))
        binding.tvPolarRng.text = String.format(polarRng, satPos.range)
        binding.tvPolarAlt.text = String.format(polarAlt, satPos.altitude)
    }
}
