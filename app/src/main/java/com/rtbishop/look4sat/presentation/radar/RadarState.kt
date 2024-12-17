package com.rtbishop.look4sat.presentation.radar

import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.predict.OrbitalPos

data class RadarState(
    val currentPass: OrbitalPass?,
    val currentTime: String,
    val isCurrentTimeAos: Boolean,
    val orientationValues: Pair<Float, Float>,
    val orbitalPos: OrbitalPos?,
    val satTrack: List<OrbitalPos>,
    val shouldShowSweep: Boolean,
    val shouldUseCompass: Boolean,
    val transmitters: List<SatRadio>,
    val sendAction: (RadarAction) -> Unit
)

sealed class RadarAction {
    data class AddToCalendar(val name: String, val aosTime: Long, val losTime: Long) : RadarAction()
}
