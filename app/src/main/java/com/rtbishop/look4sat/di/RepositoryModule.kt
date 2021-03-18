/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.di

import android.content.Context
import androidx.room.Room
import com.rtbishop.look4sat.data.database.MIGRATION_1_2
import com.rtbishop.look4sat.data.database.RoomConverters
import com.rtbishop.look4sat.data.database.SatelliteDao
import com.rtbishop.look4sat.data.database.SatelliteDb
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    fun provideSatDataDao(db: SatelliteDb): SatelliteDao {
        return db.satelliteDao()
    }
    
    @Provides
    fun provideSatelliteDb(@ApplicationContext context: Context, moshi: Moshi): SatelliteDb {
        RoomConverters.initialize(moshi)
        return Room.databaseBuilder(context, SatelliteDb::class.java, "satDb")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
