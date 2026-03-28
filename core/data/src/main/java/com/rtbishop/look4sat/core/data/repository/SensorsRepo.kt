/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.data.repository

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.RAD2DEG
import com.rtbishop.look4sat.core.domain.repository.ISensorsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.round

class SensorsRepo(
    private val sensorManager: SensorManager,
    private val sensor: Sensor?,
    private val windowManager: WindowManager
) : SensorEventListener, ISensorsRepo {

    private val _orientation = MutableStateFlow(Pair(0f, 0f))
    private val rotationMatrix = FloatArray(9)
    private val tempMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var smoothAzimuth = 0f
    private var smoothPitch = 0f
    private var hasInitialReading = false

    companion object {
        private const val SMOOTHING_FACTOR = 0.15f
    }

    override val orientation: StateFlow<Pair<Float, Float>> = _orientation

    override fun getMagDeclination(geoPos: GeoPos, time: Long): Float {
        return GeomagneticField(
            geoPos.latitude.toFloat(),
            geoPos.longitude.toFloat(),
            geoPos.altitude.toFloat(),
            time
        ).declination
    }

    override fun enableSensor() {
        hasInitialReading = false
        sensor?.let { sensorManager.registerListener(this, it, 8000) }
    }

    override fun disableSensor() = sensorManager.unregisterListener(this)

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == sensor) updateOrientation(event.values)
    }

    private fun getDisplayRotation(): Int {
        return try {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        } catch (_: Exception) {
            Surface.ROTATION_0
        }
    }

    private fun remapForRotation(rotation: Int) {
        val remapped = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, tempMatrix
            )

            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, tempMatrix
            )

            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, tempMatrix
            )

            else -> false
        }
        if (remapped) System.arraycopy(tempMatrix, 0, rotationMatrix, 0, 9)
    }

    private fun updateOrientation(rotationVector: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        remapForRotation(getDisplayRotation())
        SensorManager.getOrientation(rotationMatrix, orientationValues)
        val azimuth = (orientationValues[0] * RAD2DEG).toFloat()
        val pitch = (orientationValues[1] * RAD2DEG).toFloat()
        val magneticAzimuth = (azimuth + 360f) % 360f

        if (!hasInitialReading) {
            smoothAzimuth = magneticAzimuth
            smoothPitch = pitch
            hasInitialReading = true
        } else {
            smoothAzimuth = lowPassAngle(smoothAzimuth, magneticAzimuth)
            smoothPitch = lowPass(smoothPitch, pitch)
        }

        _orientation.value = Pair(
            round(smoothAzimuth * 10) / 10,
            round(smoothPitch * 10) / 10
        )
    }

    /** Standard exponential low-pass filter. */
    private fun lowPass(previous: Float, current: Float): Float {
        return previous + SMOOTHING_FACTOR * (current - previous)
    }

    /**
     * Low-pass filter that accounts for the 0°/360° wraparound.
     * Always takes the shortest angular path between the two values.
     */
    private fun lowPassAngle(previous: Float, current: Float): Float {
        var delta = current - previous
        // Normalise delta into the range (-180, 180]
        while (delta > 180f) delta -= 360f
        while (delta <= -180f) delta += 360f
        return (previous + SMOOTHING_FACTOR * delta + 360f) % 360f
    }
}
