package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition

interface PreferenceSource {

    fun positionToQTH(lat: Double, lon: Double): String?

    fun loadStationPosition(): StationPosition

    fun saveStationPosition(position: StationPosition)

    fun updatePositionFromGPS(): Boolean

    fun updatePositionFromQTH(qthString: String): Boolean

    fun getMagDeclination(): Float

    fun getHoursAhead(): Int

    fun getMinElevation(): Double

    fun shouldUseTextLabels(): Boolean

    fun shouldUseUTC(): Boolean

    fun shouldUseCompass(): Boolean

    fun isFirstLaunch(): Boolean

    fun setFirstLaunchDone()

    fun saveModesSelection(modes: List<String>)

    fun loadModesSelection(): List<String>

    fun getRotatorServer(): Pair<String, Int>?
}
