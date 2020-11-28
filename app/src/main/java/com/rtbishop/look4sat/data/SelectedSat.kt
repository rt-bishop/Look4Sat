package com.rtbishop.look4sat.data

import com.github.amsacode.predict4java.Position
import org.osmdroid.views.overlay.Overlay

data class SelectedSat(
    val pass: SatPass,
    val catNum: Int,
    val name: String,
    val range: Double,
    val altitude: Double,
    val velocity: Double,
    val qthLoc: String,
    val osmPos: Position,
    val coverage: Double,
    val footprint: Overlay,
    val groundTrack: Overlay
)