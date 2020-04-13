package com.rtbishop.look4sat.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.amsacode.predict4java.TLE

@Entity(tableName = "entries")
data class SatEntry(val tle: TLE, var isSelected: Boolean = false) {
    @PrimaryKey
    var catNum: Int = tle.catnum
    var name: String = tle.name
}
