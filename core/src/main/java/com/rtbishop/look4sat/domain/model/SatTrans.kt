package com.rtbishop.look4sat.domain.model

data class SatTrans(
    val uuid: String,
    val info: String,
    val isAlive: Boolean,
    var downlink: Long?,
    var uplink: Long?,
    val mode: String?,
    val isInverted: Boolean,
    val catNum: Int
)
