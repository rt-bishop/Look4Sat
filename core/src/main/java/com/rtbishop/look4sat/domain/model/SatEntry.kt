package com.rtbishop.look4sat.domain.model

import com.rtbishop.look4sat.domain.predict4kotlin.TLE

data class SatEntry(
    val tle: TLE,
    val catNum: Int = tle.catnum,
    val name: String = tle.name,
    var isSelected: Boolean = false
)
