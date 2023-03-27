package com.rtbishop.look4sat.domain

import kotlinx.coroutines.flow.StateFlow

interface ISensorRepository {

    val orientation: StateFlow<Pair<Float, Float>>

    fun enableSensor()

    fun disableSensor()
}