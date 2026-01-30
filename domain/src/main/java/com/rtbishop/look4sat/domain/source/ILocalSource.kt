/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.domain.source

import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.OrbitalObject

interface ILocalSource {
    suspend fun getEntriesTotal(): Int
    suspend fun getEntriesList(): List<SatItem>
    suspend fun getEntriesWithIds(ids: List<Int>): List<OrbitalObject>
    suspend fun insertEntries(entries: List<OrbitalData>)
    suspend fun deleteEntries()
    suspend fun getIdsWithModes(modes: List<String>): List<Int>
    suspend fun getRadiosTotal(): Int
    suspend fun getRadiosWithId(id: Int): List<SatRadio>
    suspend fun insertRadios(radios: List<SatRadio>)
    suspend fun deleteRadios()
}
