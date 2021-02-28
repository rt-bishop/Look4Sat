/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.rtbishop.look4sat.repository.localData.EntriesDao
import com.rtbishop.look4sat.repository.localData.SatelliteDb
import com.rtbishop.look4sat.repository.localData.SourcesDao
import com.rtbishop.look4sat.repository.localData.TransmittersDao
import com.rtbishop.look4sat.utility.RoomConverters
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun getContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
    
    @Provides
    @Singleton
    fun getEntriesDao(db: SatelliteDb): EntriesDao {
        return db.entriesDao()
    }
    
    @Provides
    @Singleton
    fun getSourcesDao(db: SatelliteDb): SourcesDao {
        return db.sourcesDao()
    }
    
    @Provides
    @Singleton
    fun getTransmittersDao(db: SatelliteDb): TransmittersDao {
        return db.transmittersDao()
    }
    
    @Provides
    @Singleton
    fun getSatelliteDb(@ApplicationContext context: Context, moshi: Moshi): SatelliteDb {
        RoomConverters.initialize(moshi)
        return Room.databaseBuilder(context, SatelliteDb::class.java, "satDb").build()
    }
}
