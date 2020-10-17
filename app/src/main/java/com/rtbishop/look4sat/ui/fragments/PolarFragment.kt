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
import android.hardware.*
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentPolarBinding
import com.rtbishop.look4sat.ui.adapters.TransAdapter
import com.rtbishop.look4sat.ui.views.PolarView
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.RecyclerDivider
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class PolarFragment : Fragment(R.layout.fragment_polar), SensorEventListener {

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var transmitterAdapter: TransAdapter
    private lateinit var binding: FragmentPolarBinding
    private lateinit var satPass: SatPass
    private lateinit var sensorManager: SensorManager
    private val viewModel: SharedViewModel by activityViewModels()
    private val args: PolarFragmentArgs by navArgs()
    private var magneticDeclination = 0f
    private var polarView: PolarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPolarBinding.bind(view)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        transmitterAdapter = TransAdapter(requireContext())
        binding.recycler.apply {
            setHasFixedSize(true)
            adapter = transmitterAdapter
            isVerticalScrollBarEnabled = false
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(RecyclerDivider(R.drawable.rec_divider_dark))
        }
        calculateMagneticDeclination()
        observePasses()
    }

    override fun onResume() {
        super.onResume()
        if (prefsManager.shouldUseCompass()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (prefsManager.shouldUseCompass()) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { calculateAzimuth(it) }
    }

    private fun calculateAzimuth(event: SensorEvent) {
        val rotationValues = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationValues, event.values)
        val orientationValues = FloatArray(3)
        SensorManager.getOrientation(rotationValues, orientationValues)
        val magneticAzimuth = ((orientationValues[0] * 57.2957795f) + 360f) % 360f
        val roundedAzimuth = round(magneticAzimuth * 100) / 100
        polarView?.rotation = -(roundedAzimuth + magneticDeclination)
    }

    private fun observePasses() {
        viewModel.getPasses().observe(viewLifecycleOwner, { result ->
            if (result is Result.Success) {
                satPass = result.data[args.satPassIndex]
                polarView = PolarView(requireContext(), satPass)
                binding.frame.addView(polarView)
                observeTimer()
                observeTransmitters()
            }
        })
    }

    private fun observeTimer() {
        viewModel.getCurrentTimeMillis().observe(viewLifecycleOwner, {
            transmitterAdapter.notifyDataSetChanged()
            setPassText(Date(it))
        })
    }

    private fun observeTransmitters() {
        viewModel.getTransmittersForSat(satPass.tle.catnum).observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                transmitterAdapter.setPredictor(satPass.predictor)
                transmitterAdapter.setList(it)
                binding.recycler.visibility = View.VISIBLE
                binding.noTransMsg.visibility = View.INVISIBLE
            } else {
                binding.recycler.visibility = View.INVISIBLE
                binding.noTransMsg.visibility = View.VISIBLE
            }
        })
    }

    private fun calculateMagneticDeclination() {
        prefsManager.getStationPosition().let {
            magneticDeclination = GeomagneticField(
                it.latitude.toFloat(),
                it.longitude.toFloat(),
                it.heightAMSL.toFloat(),
                System.currentTimeMillis()
            ).declination
        }
    }

    private fun setPassText(date: Date) {
        val satPos = satPass.predictor.getSatPos(date)
        val polarAz = getString(R.string.pat_azimuth)
        val polarEl = getString(R.string.pat_elevation)
        val polarRng = getString(R.string.pat_distance)
        val polarAlt = getString(R.string.pat_altitude)
        binding.azimuth.text = String.format(polarAz, Math.toDegrees(satPos.azimuth))
        binding.elevation.text = String.format(polarEl, Math.toDegrees(satPos.elevation))
        binding.distance.text = String.format(polarRng, satPos.range)
        binding.altitude.text = String.format(polarAlt, satPos.altitude)
    }
}
