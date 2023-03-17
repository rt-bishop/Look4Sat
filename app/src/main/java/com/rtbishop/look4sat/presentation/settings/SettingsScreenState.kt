package com.rtbishop.look4sat.presentation.settings

import com.rtbishop.look4sat.model.StationPos

data class LocationSettings(
    val isUpdating: Boolean,
    val stationPos: StationPos,
    val setGpsLoc: () -> Unit,
    val setManualLoc: (Double, Double) -> Unit,
    val setQthLoc: (String) -> Unit
)

data class DataSettings(
    val isUpdating: Boolean,
    val lastUpdated: Long,
    val satsTotal: Int,
    val radiosTotal: Int,
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
