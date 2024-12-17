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
package com.rtbishop.look4sat.data.repository

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.RAD2DEG
import com.rtbishop.look4sat.domain.repository.ISensorsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.round

class SensorsRepo(private val sensorManager: SensorManager, private val sensor: Sensor?) :
    SensorEventListener, ISensorsRepo {

    private val _orientation = MutableStateFlow(Pair(0f, 0f))
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE

    override val orientation: StateFlow<Pair<Float, Float>> = _orientation

    override fun getMagDeclination(geoPos: GeoPos, time: Long): Float {
        val latitude = geoPos.latitude.toFloat()
        val longitude = geoPos.longitude.toFloat()
        return GeomagneticField(latitude, longitude, geoPos.altitude.toFloat(), time).declination
    }

    override fun enableSensor() {
        sensor?.let { sensorManager.registerListener(this, it, 8000) }
    }

    override fun disableSensor() = sensorManager.unregisterListener(this)

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        sensorAccuracy = accuracy
    }

    override fun onSensorChanged(event: SensorEvent) = when {
        sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE -> {}
        event.sensor == sensor -> updateOrientation(event.values)
        else -> {}
    }

    private fun updateOrientation(rotationVector: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        SensorManager.getOrientation(rotationMatrix, orientationValues)
        val azimuth = (orientationValues[0] * RAD2DEG).toFloat()
        val pitch = (orientationValues[1] * RAD2DEG).toFloat() // roll [2]
        val magneticAzimuth = (azimuth + 360f) % 360f
        val roundedAzimuth = round(magneticAzimuth * 10) / 10
        val roundedPitch = round(pitch * 10) / 10
        _orientation.value = Pair(roundedAzimuth, roundedPitch)
    }
}
