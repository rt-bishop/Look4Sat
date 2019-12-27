/*
 * LookingSat. Amateur radio & weather satellite tracker and passes calculator.
 * Copyright (C) 2019 Arty Bishop (bishop.arty@gmail.com)
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
import com.rtbishop.look4sat.storage.TransmittersDao
import com.rtbishop.look4sat.storage.TransmittersDb
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