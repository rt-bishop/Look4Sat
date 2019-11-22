package com.rtbishop.lookingsat.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rtbishop.lookingsat.repo.Transmitter

@Database(entities = [Transmitter::class], version = 1, exportSchema = false)
abstract class TransmittersDb : RoomDatabase() {

    abstract fun transmittersDao(): TransmittersDao

    companion object {

        @Volatile
        private var INSTANCE: TransmittersDb? = null

        fun getInstance(context: Context): TransmittersDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    TransmittersDb::class.java,
                    "transmitters"
                ).build()
            }
    }
}