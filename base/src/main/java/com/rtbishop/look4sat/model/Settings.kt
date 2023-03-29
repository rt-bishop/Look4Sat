package com.rtbishop.look4sat.model

data class OtherSettings(
    val utcState: Boolean,
    val updateState: Boolean,
    val sweepState: Boolean,
    val sensorState: Boolean
)

data class DatabaseState(val entriesTotal: Int, val radiosTotal: Int, val timestamp: Long)
