package com.rtbishop.look4sat.presentation.settings

import com.rtbishop.look4sat.domain.predict.GeoPos

data class PositionSettings(val isUpdating: Boolean, val stationPos: GeoPos, val messageResId: Int)

data class DataSettings(val isUpdating: Boolean, val entriesTotal: Int, val radiosTotal: Int, val timestamp: Long)
