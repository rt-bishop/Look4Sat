package com.rtbishop.look4sat.framework.model

import com.squareup.moshi.Json

data class OMM(
    @field:Json(name = "OBJECT_NAME") val name: String,
    @field:Json(name = "EPOCH") val epochString: String,
    @field:Json(name = "MEAN_MOTION") val meanmo: Double,
    @field:Json(name = "ECCENTRICITY") val eccn: Double,
    @field:Json(name = "INCLINATION") val incl: Double,
    @field:Json(name = "RA_OF_ASC_NODE") val raan: Double,
    @field:Json(name = "ARG_OF_PERICENTER") val argper: Double,
    @field:Json(name = "MEAN_ANOMALY") val meanan: Double,
    @field:Json(name = "NORAD_CAT_ID") val catnum: Int,
    @field:Json(name = "BSTAR") val bstar: Double,
)
