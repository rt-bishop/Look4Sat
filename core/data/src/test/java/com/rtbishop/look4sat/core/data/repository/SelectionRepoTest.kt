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
package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.domain.model.DataSourcesSettings
import com.rtbishop.look4sat.core.domain.model.DatabaseState
import com.rtbishop.look4sat.core.domain.model.OtherSettings
import com.rtbishop.look4sat.core.domain.model.PassesSettings
import com.rtbishop.look4sat.core.domain.model.RCSettings
import com.rtbishop.look4sat.core.domain.model.RadioControlSettings
import com.rtbishop.look4sat.core.domain.model.SatItem
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionRepoTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `unknown mode values do not crash and do not filter out entries`() = runTest(dispatcher) {
        val localSource = FakeLocalSource(
            entries = listOf(
                SatItem(25544, "ISS (ZARYA)", false),
                SatItem(40967, "TIANGONG", false)
            )
        )
        val settingsRepo = FakeSettingsRepo(selectedModes = listOf("REMOVED_MODE"))
        val repository = SelectionRepo(dispatcher, localSource, settingsRepo)

        val flow = repository.getEntriesFlow()
        repository.setModes(listOf("REMOVED_MODE"))

        val items = flow.first()

        assertEquals(listOf(25544, 40967), items.map { it.catnum })
        assertEquals(listOf("REMOVED_MODE"), repository.getCurrentModes())
    }

    @Test
    fun `selected satellites are shown first`() = runTest(dispatcher) {
        val localSource = FakeLocalSource(
            entries = listOf(
                SatItem(44444, "Zeta", false),
                SatItem(25544, "Alpha", false),
                SatItem(40967, "Beta", false)
            )
        )
        val settingsRepo = FakeSettingsRepo(selectedModes = emptyList())
        val repository = SelectionRepo(dispatcher, localSource, settingsRepo)

        val flow = repository.getEntriesFlow()
        repository.setSelection(listOf(40967), true)

        val items = flow.first()

        assertEquals(listOf(40967, 25544, 44444), items.map { it.catnum })
        assertEquals(listOf(true, false, false), items.map { it.isSelected })
    }

    private class FakeLocalSource(
        private val entries: List<SatItem>
    ) : ILocalSource {
        override suspend fun getEntriesTotal(): Int = entries.size

        override suspend fun getEntriesList(): List<SatItem> = entries

        override suspend fun getEntriesWithIds(ids: List<Int>): List<OrbitalObject> = emptyList()

        override suspend fun insertEntries(entries: List<com.rtbishop.look4sat.core.domain.predict.OrbitalData>) = Unit

        override suspend fun deleteEntries() = Unit

        override suspend fun getIdsWithModes(modes: List<String>): List<Int> = emptyList()

        override suspend fun getRadiosTotal(): Int = 0

        override suspend fun getRadiosWithId(id: Int): List<SatRadio> = emptyList()

        override suspend fun insertRadios(radios: List<SatRadio>) = Unit

        override suspend fun deleteRadios() = Unit
    }

    private class FakeSettingsRepo(
        selectedModes: List<String>
    ) : ISettingsRepo {

        override val appVersionName: String = "test"

        override val selectedIds: StateFlow<List<Int>> = MutableStateFlow(emptyList())

        override val selectedSatModes: MutableStateFlow<List<String>> = MutableStateFlow(selectedModes)

        override val passesSettings: StateFlow<PassesSettings> = MutableStateFlow(
            PassesSettings(hoursAhead = 24, minElevation = 0.0)
        )

        override val stationPosition: StateFlow<GeoPos> = MutableStateFlow(GeoPos(0.0, 0.0))

        override val databaseState: MutableStateFlow<DatabaseState> = MutableStateFlow(DatabaseState(0, 0, 0L))

        override val rcSettings: StateFlow<RCSettings> = MutableStateFlow(
            RCSettings(false, "", "", "", false, "", "", "", 0L, false, "", "", "", false, "", "")
        )

        override val otherSettings: StateFlow<OtherSettings> = MutableStateFlow(
            OtherSettings(false, false, false, false, false, false, false, false)
        )

        override val dataSourcesSettings: StateFlow<DataSourcesSettings> = MutableStateFlow(
            DataSourcesSettings(emptyList(), emptyList())
        )

        override val dataSourcesStatus: StateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())

        override val radioControlSettings: StateFlow<RadioControlSettings> = MutableStateFlow(
            RadioControlSettings(false, RadioControlSettings.MODEL_YAESU_FT817, "", "", "", "", 9600)
        )

        override fun setSelectedIds(ids: List<Int>) = Unit

        override fun setSelectedSatModes(modes: List<String>) {
            selectedSatModes.value = modes
        }

        override fun setPassesSettings(settings: PassesSettings) = Unit

        override fun setStationPosition(latitude: Double, longitude: Double, altitude: Double): Boolean = true

        override fun setStationPosition(): Boolean = true

        override fun setStationPosition(locator: String): Boolean = true

        override fun updateDatabaseState(state: DatabaseState) {
            databaseState.value = state
        }

        override fun updateRCSettings(settings: RCSettings) = Unit

        override fun updateOtherSettings(transform: (OtherSettings) -> OtherSettings) = Unit

        override fun updateDataSourcesSettings(settings: DataSourcesSettings) = Unit

        override fun updateDataSourcesStatus(status: Map<String, Int>) = Unit

        override fun updateRadioControlSettings(settings: RadioControlSettings) = Unit

        override fun getSatelliteOffset(catnum: Int): String = ""

        override fun setSatelliteOffset(catnum: Int, offset: String) = Unit

        override fun getAmSatCallsign(): String = ""

        override fun setAmSatCallsign(callsign: String) = Unit
    }
}


