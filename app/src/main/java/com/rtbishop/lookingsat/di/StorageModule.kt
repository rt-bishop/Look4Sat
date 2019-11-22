package com.rtbishop.lookingsat.di

import android.content.Context
import androidx.room.Room
import com.rtbishop.lookingsat.storage.TransmittersDao
import com.rtbishop.lookingsat.storage.TransmittersDb
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class StorageModule {

    @Singleton
    @Provides
    fun provideTransmittersDb(context: Context): TransmittersDb {
        return Room.databaseBuilder(context, TransmittersDb::class.java, "transmitters")
            .build()
    }

    @Singleton
    @Provides
    fun provideTransmittersDao(db: TransmittersDb): TransmittersDao {
        return db.transmittersDao()
    }
}