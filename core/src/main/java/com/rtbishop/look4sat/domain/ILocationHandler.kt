package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import kotlinx.coroutines.flow.SharedFlow

interface ILocationHandler {

    val stationPosition: SharedFlow<DataState<GeoPos>>

    fun getStationLocator(): String

    fun getStationPosition(): GeoPos

    fun setStationPosition(latitude: Double, longitude: Double)

    fun setPositionFromGps()

    fun setPositionFromNet()

    fun setPositionFromQth(locator: String)

    fun setPositionHandled()
}
