package com.rtbishop.look4sat.presentation.satPassInfoScreen

import com.rtbishop.look4sat.domain.SatPos

data class PassData(val position: SatPos, val positions: List<SatPos> = emptyList())
