package com.rtbishop.look4sat.data

import kotlinx.coroutines.flow.StateFlow

interface ISensorSource {

    val orientation: StateFlow<Pair<Float, Float>>

    fun enableSensor()

    fun disableSensor()
}
