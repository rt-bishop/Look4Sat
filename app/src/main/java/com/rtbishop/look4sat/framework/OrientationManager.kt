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
package com.rtbishop.look4sat.framework

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.rtbishop.look4sat.domain.predict.RAD2DEG
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

@Singleton
class OrientationManager @Inject constructor(private val sensorManager: SensorManager) :
    SensorEventListener {

    interface OrientationListener {
        fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float)
    }

    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var orientationListener: OrientationListener? = null

    fun startListening(listener: OrientationListener) {
        if (sensor != null && orientationListener !== listener) {
            orientationListener = listener
            sensorManager.registerListener(this, sensor, 16000)
        } else return
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        orientationListener = null
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        sensorAccuracy = accuracy
    }

    override fun onSensorChanged(event: SensorEvent) {
        when {
            orientationListener == null -> return
            sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE -> return
            event.sensor == sensor -> updateOrientation(event.values)
        }
    }

    private fun updateOrientation(rotationVector: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        SensorManager.getOrientation(rotationMatrix, orientationValues)
        val azimuth = (orientationValues[0] * RAD2DEG).toFloat()
        val pitch = (orientationValues[1] * RAD2DEG).toFloat()
        val roll = (orientationValues[2] * RAD2DEG).toFloat()
        val magneticAzimuth = (azimuth + 360f) % 360f
        val roundedAzimuth = round(magneticAzimuth * 10) / 10
        val roundedPitch = round(pitch * 10) / 10
        val roundedRoll = round(roll * 10) / 10
        orientationListener?.onOrientationChanged(roundedAzimuth, roundedPitch, roundedRoll)
    }
}
