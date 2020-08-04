package com.rtbishop.look4sat.data

import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.overlay.OverlayItem

data class SatOverlayItem(
    val name: String,
    val description: String,
    val position: IGeoPoint,
    val pass: SatPass
) : OverlayItem(name, description, position)