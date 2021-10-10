package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.domain.predict.GeoPos
import kotlinx.coroutines.flow.SharedFlow

interface LocationProvider {

    val updatedLocation: SharedFlow<GeoPos?>
}
