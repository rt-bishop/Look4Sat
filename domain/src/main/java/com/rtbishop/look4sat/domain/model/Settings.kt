package com.rtbishop.look4sat.domain.model

data class DatabaseState(val entriesTotal: Int, val radiosTotal: Int, val timestamp: Long)

data class PassesSettings(val hoursAhead: Int, val minElevation: Double)

data class OtherSettings(
    val utcState: Boolean,
    val updateState: Boolean,
    val sweepState: Boolean,
    val sensorState: Boolean
)
