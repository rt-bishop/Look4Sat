package com.rtbishop.look4sat.domain.model

data class DatabaseState(
    val numberOfRadios: Int,
    val numberOfSatellites: Int,
    val updateTimestamp: Long
)

data class PassesSettings(
    val hoursAhead: Int,
    val minElevation: Double,
    val selectedModes: List<String>
)

data class RCSettings(
    val rotatorState: Boolean,
    val rotatorAddress: String,
    val rotatorPort: String,
    val bluetoothState: Boolean,
    val bluetoothFormat: String,
    val bluetoothName: String,
    val bluetoothAddress: String,
)

data class OtherSettings(
    val stateOfAutoUpdate: Boolean,
    val stateOfSensors: Boolean,
    val stateOfSweep: Boolean,
    val stateOfUtc: Boolean,
    val stateOfLightTheme: Boolean,
    val shouldSeeWarning: Boolean,
    val shouldSeeWelcome: Boolean
)
