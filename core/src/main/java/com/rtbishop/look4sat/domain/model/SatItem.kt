package com.rtbishop.look4sat.domain.model

data class SatItem(
    val catNum: Int,
    val name: String,
    var isSelected: Boolean,
    val modes: List<String>
)
