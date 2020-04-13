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

package com.rtbishop.look4sat.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.Transmitter
import com.rtbishop.look4sat.persistence.dao.EntriesDao
import com.rtbishop.look4sat.persistence.dao.TransmittersDao
import com.rtbishop.look4sat.utility.Converters

@Database(entities = [Transmitter::class, SatEntry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SatelliteDb : RoomDatabase() {

    abstract fun entriesDao(): EntriesDao

    abstract fun transmittersDao(): TransmittersDao
}
