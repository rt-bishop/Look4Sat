package com.rtbishop.look4sat.presentation.settings

data class LocationSettings(
    val getUpdating: Boolean,
    val getLastUpdated: Long,
    val getLatitude: Double,
    val getLongitude: Double,
    val getLocator: String,
    val setGpsLoc: () -> Unit,
    val setManualLoc: (Double, Double) -> Unit,
    val setQthLoc: (String) -> Unit
)

data class DataSettings(
    val getUpdating: Boolean,
    val getLastUpdated: Long,
    val getSatellites: Int,
    val getRadios: Int,
    val updateFromWeb: () -> Unit,
    val updateFromFile: (String) -> Unit,
    val clearAllData: () -> Unit
)

data class OtherSettings(
    val getUtc: Boolean,
    val getUpdate: Boolean,
    val getSweep: Boolean,
    val getSensor: Boolean,
    val setUtc: (Boolean) -> Unit,
    val setUpdate: (Boolean) -> Unit,
    val setSweep: (Boolean) -> Unit,
    val setSensor: (Boolean) -> Unit
)
