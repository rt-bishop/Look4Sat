package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition

interface LocationSource {
    
    fun getLastKnownLocation(): StationPosition?

    fun getMagDeclination(position: StationPosition): Float

    fun loadStationPosition(): StationPosition

    fun saveStationPosition(position: StationPosition)
}