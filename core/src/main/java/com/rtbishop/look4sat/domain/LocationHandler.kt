package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import kotlinx.coroutines.flow.SharedFlow

interface LocationHandler {

    val stationPosition: SharedFlow<DataState<GeoPos>>

    fun getStationPosition(): GeoPos

    fun setStationPosition(latitude: Double, longitude: Double)

    fun setPositionFromLocation()

    fun setPositionFromNet()

    fun setPositionFromGps()

    fun setPositionFromQth(qthString: String)
}
