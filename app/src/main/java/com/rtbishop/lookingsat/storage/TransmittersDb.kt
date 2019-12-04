package com.rtbishop.lookingsat.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rtbishop.lookingsat.repo.Transmitter

@Database(entities = [Transmitter::class], version = 1, exportSchema = false)
abstract class TransmittersDb : RoomDatabase() {

    abstract fun transmittersDao(): TransmittersDao
}