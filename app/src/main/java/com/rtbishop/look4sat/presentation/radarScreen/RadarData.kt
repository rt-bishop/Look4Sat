package com.rtbishop.look4sat.presentation.radarScreen

import com.rtbishop.look4sat.domain.SatPos

data class RadarData(val satPos: SatPos, val satTrack: List<SatPos> = emptyList())
