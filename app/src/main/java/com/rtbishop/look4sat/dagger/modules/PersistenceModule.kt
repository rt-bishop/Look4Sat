/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.dagger.modules

import android.content.Context
import androidx.room.Room
import com.rtbishop.look4sat.persistence.LocalDataSource
import com.rtbishop.look4sat.persistence.LocalSource
import com.rtbishop.look4sat.persistence.SatelliteDb
import com.rtbishop.look4sat.persistence.dao.EntriesDao
import com.rtbishop.look4sat.persistence.dao.TransmittersDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PersistenceModule {

    @Singleton
    @Provides
    fun provideEntriesDao(db: SatelliteDb): EntriesDao {
        return db.entriesDao()
    }

    @Singleton
    @Provides
    fun provideTransmittersDao(db: SatelliteDb): TransmittersDao {
        return db.transmittersDao()
    }

    @Singleton
    @Provides
    fun provideLocalDataSource(
        entriesDao: EntriesDao,
        transmittersDao: TransmittersDao
    ): LocalSource {
        return LocalDataSource(entriesDao, transmittersDao)
    }

    @Singleton
    @Provides
    fun provideSatelliteDb(context: Context): SatelliteDb {
        return Room.databaseBuilder(context, SatelliteDb::class.java, "satellites")
            .build()
    }
}
