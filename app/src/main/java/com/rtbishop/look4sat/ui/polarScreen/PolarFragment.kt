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
package com.rtbishop.look4sat.ui.polarScreen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentPolarBinding
import com.rtbishop.look4sat.repository.PrefsRepo
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.formatForTimer
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class PolarFragment : Fragment(R.layout.fragment_polar), SensorEventListener {

    @Inject
    lateinit var prefsRepo: PrefsRepo

    private lateinit var transmitterAdapter: TransAdapter
    private lateinit var binding: FragmentPolarBinding
    private lateinit var satPass: SatPass
    private lateinit var sensorManager: SensorManager
    private val viewModel: SharedViewModel by activityViewModels()
    private var magneticDeclination = 0f
    private var polarView: PolarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPolarBinding.bind(view)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticDeclination = prefsRepo.getMagDeclination()
        observePasses()
    }

    override fun onResume() {
        super.onResume()
        if (prefsRepo.shouldUseCompass()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (prefsRepo.shouldUseCompass()) {
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
        viewModel.passes.observe(viewLifecycleOwner, { result ->
            if (result is Result.Success) {
                satPass = result.data[requireArguments().getInt("index")]
                polarView = PolarView(requireContext()).apply { setPass(satPass) }
                binding.frame.addView(polarView)
                observeTransmitters()
            }
        })
    }

    private fun observeTransmitters() {
        viewModel.getTransmittersForSat(satPass.tle.catnum).observe(viewLifecycleOwner, { list ->
            transmitterAdapter = TransAdapter(requireContext(), satPass)
            if (list.isNotEmpty()) {
                transmitterAdapter.setData(list)
                binding.recycler.apply {
                    setHasFixedSize(true)
                    adapter = transmitterAdapter
                    isVerticalScrollBarEnabled = false
                    layoutManager = LinearLayoutManager(context)
                    (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                    addItemDecoration(RecyclerDivider(R.drawable.rec_divider_dark))
                    visibility = View.VISIBLE
                }
                binding.noTransMsg.visibility = View.INVISIBLE
            } else {
                binding.recycler.visibility = View.INVISIBLE
                binding.noTransMsg.visibility = View.VISIBLE
            }
            observeTimer()
        })
    }

    private fun observeTimer() {
        viewModel.getAppTimer().observe(viewLifecycleOwner, {
            setPassText(it)
            polarView?.invalidate()
            transmitterAdapter.tickTransmitters()
        })
    }

    private fun setPassText(timeNow: Long) {
        val dateNow = Date(timeNow)
        val satPos = satPass.predictor.getSatPos(dateNow)
        val polarAz = getString(R.string.pat_azimuth)
        val polarEl = getString(R.string.pat_elevation)
        val polarRng = getString(R.string.pat_distance)
        val polarAlt = getString(R.string.pat_altitude)
        binding.azimuth.text = String.format(polarAz, Math.toDegrees(satPos.azimuth))
        binding.elevation.text = String.format(polarEl, Math.toDegrees(satPos.elevation))
        binding.distance.text = String.format(polarRng, satPos.range)
        binding.altitude.text = String.format(polarAlt, satPos.altitude)

        if (!satPass.tle.isDeepspace) {
            if (dateNow.before(satPass.pass.startTime)) {
                val millisBeforeStart = satPass.pass.startTime.time.minus(timeNow)
                binding.polarTimer.text = millisBeforeStart.formatForTimer()
            } else {
                val millisBeforeEnd = satPass.pass.endTime.time.minus(timeNow)
                binding.polarTimer.text = millisBeforeEnd.formatForTimer()
                if (dateNow.after(satPass.pass.endTime)) {
                    binding.polarTimer.text = 0L.formatForTimer()
                    findNavController().popBackStack()
                }
            }
        } else {
            binding.polarTimer.text = 0L.formatForTimer()
        }
    }
}
