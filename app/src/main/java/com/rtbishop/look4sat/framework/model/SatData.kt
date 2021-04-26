package com.rtbishop.look4sat.framework.model

import com.rtbishop.look4sat.domain.predict4kotlin.Position
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite

data class SatData(
    val pass: Satellite,
    val catNum: Int,
    val name: String,
    val range: Double,
    val altitude: Double,
    val velocity: Double,
    val qthLoc: String,
    val osmPos: Position
)