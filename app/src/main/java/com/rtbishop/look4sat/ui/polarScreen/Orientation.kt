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

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.round

class Orientation @Inject constructor(
    private val mSensorManager: SensorManager,
    private val mWindowManager: WindowManager
) : SensorEventListener {

    interface OrientationListener {
        fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float)
    }

    private val mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val mRotationMatrix = FloatArray(9)
    private val mOrientationValues = FloatArray(3)
    private val mOneRadDegrees = 57.295779513f
    private var mLastAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var mListener: OrientationListener? = null

    fun startListening(listener: OrientationListener) {
        when {
            mListener === listener -> return
            else -> {
                mListener = listener
                mSensorManager.registerListener(this, mRotationSensor, 16000)
            }
        }
    }

    fun stopListening() {
        mSensorManager.unregisterListener(this)
        mListener = null
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        mLastAccuracy = accuracy
    }

    override fun onSensorChanged(event: SensorEvent) {
        when {
            mListener == null -> return
            mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE -> return
            event.sensor == mRotationSensor -> updateOrientation(event.values)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateOrientation(rotationVector: FloatArray) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, rotationVector)
        val (matrixColumn, sense) = when (mWindowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> Pair(0, 1)
            Surface.ROTATION_90 -> Pair(1, -1)
            Surface.ROTATION_180 -> Pair(0, -1)
            Surface.ROTATION_270 -> Pair(1, 1)
            else -> error("Invalid screen rotation value")
        }
        val x = sense * mRotationMatrix[matrixColumn]
        val y = sense * mRotationMatrix[matrixColumn + 3]
        val azimuth = -atan2(y, x) * mOneRadDegrees
//        SensorManager.getOrientation(mRotationMatrix, mOrientationValues)
//        val azimuth = mOrientationValues[0] * mOneRadDegrees
        val pitch = mOrientationValues[1] * mOneRadDegrees
        val roll = mOrientationValues[2] * mOneRadDegrees
        val magneticAzimuth = (azimuth + 360f) % 360f
        val roundedAzimuth = round(magneticAzimuth * 10) / 10
        mListener?.onOrientationChanged(roundedAzimuth, pitch, roll)
    }
}