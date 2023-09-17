/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rtbishop.look4sat.data.database.entity.SatEntry
import com.rtbishop.look4sat.data.database.entity.SatRadio

@Database(entities = [SatEntry::class, SatRadio::class], version = 1, exportSchema = false)
abstract class Look4SatDb : RoomDatabase() {
    abstract fun look4SatDao(): Look4SatDao
}

//val MIGRATION_1_2 = object : Migration(1, 2) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        database.execSQL("CREATE TABLE entries_backup (name TEXT NOT NULL, epoch REAL NOT NULL, meanmo REAL NOT NULL, eccn REAL NOT NULL, incl REAL NOT NULL, raan REAL NOT NULL, argper REAL NOT NULL, meanan REAL NOT NULL, catnum INTEGER NOT NULL, bstar REAL NOT NULL, xincl REAL NOT NULL, xnodeo REAL NOT NULL, omegao REAL NOT NULL, xmo REAL NOT NULL, xno REAL NOT NULL, orbitalPeriod REAL NOT NULL, isDeepSpace INTEGER NOT NULL, comment TEXT, PRIMARY KEY(catnum))")
//        database.execSQL("INSERT INTO entries_backup (name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar, xincl, xnodeo, omegao, xmo, xno, orbitalPeriod, isDeepSpace, comment) SELECT name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar, xincl, xnodeo, omegao, xmo, xno, 1440 / meanmo, 1440 / meanmo >= 225.0, comment FROM entries")
//        database.execSQL("DROP TABLE entries")
//        database.execSQL("ALTER TABLE entries_backup RENAME TO entries")
//    }
//}
