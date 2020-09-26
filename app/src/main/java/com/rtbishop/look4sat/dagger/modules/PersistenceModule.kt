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
import com.rtbishop.look4sat.repo.local.EntriesDao
import com.rtbishop.look4sat.repo.local.SatDb
import com.rtbishop.look4sat.repo.local.SourcesDao
import com.rtbishop.look4sat.repo.local.TransDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PersistenceModule {

    @Singleton
    @Provides
    fun provideEntriesDao(db: SatDb): EntriesDao {
        return db.entriesDao()
    }

    @Singleton
    @Provides
    fun provideTransDao(db: SatDb): TransDao {
        return db.transDao()
    }

    @Singleton
    @Provides
    fun providesSourcesDao(db: SatDb): SourcesDao {
        return db.sourcesDao()
    }

    @Singleton
    @Provides
    fun provideSatDb(context: Context): SatDb {
        return Room.databaseBuilder(context, SatDb::class.java, "satDb")
            .build()
    }
}
