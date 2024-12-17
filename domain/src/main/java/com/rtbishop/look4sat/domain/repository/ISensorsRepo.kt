package com.rtbishop.look4sat.domain.repository

import com.rtbishop.look4sat.domain.predict.GeoPos
import kotlinx.coroutines.flow.StateFlow

interface ISensorsRepo {
    val orientation: StateFlow<Pair<Float, Float>>
    fun getMagDeclination(geoPos: GeoPos, time: Long = System.currentTimeMillis()): Float
    fun enableSensor()
    fun disableSensor()
}
