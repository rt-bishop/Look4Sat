package com.rtbishop.lookingsat.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rtbishop.lookingsat.repo.Transmitter

@Database(entities = [Transmitter::class], version = 1, exportSchema = false)
abstract class TransmitterDatabase : RoomDatabase() {

    abstract fun transDao(): TransmitterDao
}