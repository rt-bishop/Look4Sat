package com.rtbishop.look4sat.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ISensorsRepo {
    val orientation: StateFlow<Pair<Float, Float>>
    fun enableSensor()
    fun disableSensor()
}
