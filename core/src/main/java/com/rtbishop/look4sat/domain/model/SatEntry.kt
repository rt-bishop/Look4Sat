package com.rtbishop.look4sat.domain.model

import com.rtbishop.look4sat.predict4kotlin.TLE

data class SatEntry(
    val tle: TLE,
    var isSelected: Boolean = false,
    val catNum: Int = tle.catnum,
    val name: String = tle.name
)
