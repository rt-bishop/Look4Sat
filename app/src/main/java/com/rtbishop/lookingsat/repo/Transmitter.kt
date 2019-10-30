package com.rtbishop.lookingsat.repo

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "transmitters")
data class Transmitter(
    @PrimaryKey @SerializedName("uuid") val uuid: String,
    @SerializedName("description") val description: String,
    @SerializedName("alive") val isAlive: Boolean,
    @SerializedName("uplink_low") val uplinkLow: Long?,
    @SerializedName("uplink_high") val uplinkHigh: Long?,
    @SerializedName("downlink_low") val downlinkLow: Long?,
    @SerializedName("downlink_high") val downlinkHigh: Long?,
    @SerializedName("mode") val mode: String?,
    @SerializedName("invert") val isInverted: Boolean,
    @SerializedName("norad_cat_id") val noradCatId: Int
)
