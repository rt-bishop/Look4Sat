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

package com.rtbishop.look4sat.di

import android.content.Context
import androidx.room.Room
import com.rtbishop.look4sat.repo.local.EntriesDao
import com.rtbishop.look4sat.repo.local.SatelliteDb
import com.rtbishop.look4sat.repo.local.SourcesDao
import com.rtbishop.look4sat.repo.local.TransmittersDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
class PersistenceModule {

    @ActivityScoped
    @Provides
    fun provideEntriesDao(db: SatelliteDb): EntriesDao {
        return db.entriesDao()
    }

    @ActivityScoped
    @Provides
    fun provideTransDao(db: SatelliteDb): TransmittersDao {
        return db.transmittersDao()
    }

    @ActivityScoped
    @Provides
    fun providesSourcesDao(db: SatelliteDb): SourcesDao {
        return db.sourcesDao()
    }

    @ActivityScoped
    @Provides
    fun provideSatDb(@ActivityContext context: Context): SatelliteDb {
        return Room.databaseBuilder(context, SatelliteDb::class.java, "satDb").build()
    }
}
