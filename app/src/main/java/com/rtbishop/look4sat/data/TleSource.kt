package com.rtbishop.look4sat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class TleSource(@PrimaryKey var url: String = String())