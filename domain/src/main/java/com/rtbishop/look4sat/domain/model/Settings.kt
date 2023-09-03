package com.rtbishop.look4sat.domain.model

data class DatabaseState(val numberOfRadios: Int, val numberOfSatellites: Int, val updateTimestamp: Long)

data class PassesSettings(val filterHoursAhead: Int, val filterMinElevation: Double)

data class OtherSettings(
    val stateOfAutoUpdate: Boolean,
    val stateOfSensors: Boolean,
    val stateOfSweep: Boolean,
    val stateOfUtc: Boolean
)
