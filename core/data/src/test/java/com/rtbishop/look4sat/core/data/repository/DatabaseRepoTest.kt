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
import com.rtbishop.look4sat.core.domain.predict.OrbitalData
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import com.rtbishop.look4sat.core.domain.source.IRemoteSource
import com.rtbishop.look4sat.core.domain.source.NetworkResult
import com.rtbishop.look4sat.core.domain.utility.DataParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val dataParser = DataParser(dispatcher)

    // fixtures carry a current epoch: stale entries are pruned on every remote update
    private val todayEpoch = LocalDate.now().let { "%02d%03d".format(it.year % 100, it.dayOfYear) }
    private val fresherEpoch = "$todayEpoch.71955234".toDouble()

    @Test
    fun `manual satellite import parses csv stream from content uri`() = runTest(dispatcher) {
        val uri = "content://look4sat/import/satellites"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            fileStreams[uri] = { validCsvStream() }
        }
        val settingsRepo = FakeSettingsRepo()
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)

        repository.updateTLEFromFile(uri)

        assertEquals(1, localSource.insertedEntries.size)
        assertEquals(25544, localSource.insertedEntries.first().catnum)
        assertTrue(settingsRepo.databaseState.value.numberOfSatellites > 0)
    }

    @Test
    fun `manual satellite import keeps tle support`() = runTest(dispatcher) {
        val uri = "content://look4sat/import/legacy"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            fileStreams[uri] = { validTleStream() }
        }
        val settingsRepo = FakeSettingsRepo()
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)

        repository.updateTLEFromFile(uri)

        assertEquals(1, localSource.insertedEntries.size)
        assertEquals(25544, localSource.insertedEntries.first().catnum)
    }

    @Test
    fun `custom data source imports omm csv from web`() = runTest(dispatcher) {
        val customCsvUrl = "https://example.com/custom-omm.csv"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            networkStreams[customCsvUrl] = { validCsvStream() }
        }
        val settingsRepo = FakeSettingsRepo(
            dataSources = DataSourcesSettings(
                satelliteUrls = listOf(customCsvUrl),
                transceiversUrls = emptyList()
            )
        )
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)

        repository.updateFromRemote()

        assertTrue(localSource.insertedEntries.any { it.catnum == 25544 })
    }

    @Test
    fun `remote update takes the freshest elements and the preferred name`() = runTest(dispatcher) {
        val primaryUrl = "https://example.com/primary.txt"
        val secondaryUrl = "https://example.com/secondary.txt"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            networkStreams[primaryUrl] = { validTleStream() }
            networkStreams[secondaryUrl] = { fresherTleStream() }
        }
        val settingsRepo = FakeSettingsRepo(
            dataSources = DataSourcesSettings(
                satelliteUrls = listOf(primaryUrl, secondaryUrl),
                transceiversUrls = emptyList()
            )
        )
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)

        repository.updateFromRemote()

        val entry = localSource.insertedEntries.single { it.catnum == 25544 }
        assertEquals("ISS (ZARYA)", entry.name)
        assertEquals(fresherEpoch, entry.epoch, 1e-8)
    }

    @Test
    fun `manual satellite import does not overwrite fresher data`() = runTest(dispatcher) {
        val freshUri = "content://look4sat/import/fresh"
        val staleUri = "content://look4sat/import/stale"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            fileStreams[freshUri] = { fresherTleStream() }
            fileStreams[staleUri] = { validTleStream() }
        }
        val settingsRepo = FakeSettingsRepo()
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)

        assertEquals(1, repository.updateTLEFromFile(freshUri))
        assertEquals(1, repository.updateTLEFromFile(staleUri))
        assertEquals(fresherEpoch, localSource.insertedEntries.single().epoch, 1e-8)
    }

    @Test
    fun `remote update drops transceivers that are no longer published`() = runTest(dispatcher) {
        val radioUrl = "https://example.com/transmitters.json"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource()
        val settingsRepo = FakeSettingsRepo(
            dataSources = DataSourcesSettings(
                satelliteUrls = emptyList(),
                transceiversUrls = listOf(radioUrl)
            )
        )
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)

        remoteSource.networkStreams[radioUrl] = { twoRadiosStream() }
        repository.updateFromRemote()
        assertEquals(2, localSource.insertedRadios.size)

        remoteSource.networkStreams[radioUrl] = { singleRadioStream() }
        repository.updateFromRemote()
        assertEquals(listOf("uuid-alive"), localSource.insertedRadios.map { it.uuid })
    }

    @Test
    fun `remote update renames satellites even when their elements are not newer`() = runTest(dispatcher) {
        val preferredUrl = "https://example.com/preferred.txt"
        val freshUrl = "https://example.com/fresh.txt"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            networkStreams[freshUrl] = { fresherTleStream() }
        }
        val settingsRepo = FakeSettingsRepo(
            dataSources = DataSourcesSettings(
                satelliteUrls = listOf(freshUrl),
                transceiversUrls = emptyList()
            )
        )
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)
        repository.updateFromRemote()
        assertEquals("ISS", localSource.insertedEntries.single().name)

        // the user moves a source with older elements, but a better name, to the top of the list
        remoteSource.networkStreams[preferredUrl] = { validTleStream() }
        settingsRepo.dataSourcesSettings.value = DataSourcesSettings(
            satelliteUrls = listOf(preferredUrl, freshUrl),
            transceiversUrls = emptyList()
        )
        repository.updateFromRemote()

        val entry = localSource.insertedEntries.single()
        assertEquals("ISS (ZARYA)", entry.name)
        assertEquals(fresherEpoch, entry.epoch, 1e-8)
    }

    @Test
    fun `failed remote update records the attempt without claiming success`() = runTest(dispatcher) {
        val failingUrl = "https://example.com/offline.txt"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource()
        val settingsRepo = FakeSettingsRepo(
            dataSources = DataSourcesSettings(
                satelliteUrls = listOf(failingUrl),
                transceiversUrls = emptyList()
            )
        )
        settingsRepo.databaseState.value = DatabaseState(0, 0, 1_000L)
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)
        repository.updateFromRemote()

        // the update timestamp stays put so the data is not presented as fresh, while the recorded
        // attempt keeps the next app launch from hammering a source that is already failing
        assertEquals(1_000L, settingsRepo.databaseState.value.updateTimestamp)
    }

    @Test
    fun `remote update keeps the transceivers imported from a file`() = runTest(dispatcher) {
        val radioUri = "content://imported.json"
        val radioUrl = "https://example.com/transmitters.json"
        val localSource = FakeLocalSource()
        val remoteSource = FakeRemoteSource().apply {
            fileStreams[radioUri] = { customRadioStream() }
            networkStreams[radioUrl] = { singleRadioStream() }
        }
        val settingsRepo = FakeSettingsRepo(
            dataSources = DataSourcesSettings(
                satelliteUrls = emptyList(),
                transceiversUrls = listOf(radioUrl)
            )
        )
        val repository = DatabaseRepo(dispatcher, dataParser, localSource, remoteSource, settingsRepo)
        repository.updateTransceiversFromFile(radioUri)
        repository.updateFromRemote()

        // the remote snapshot replaces what the sources publish, the imported entry stays put
        assertEquals(setOf("uuid-custom", "uuid-alive"), localSource.insertedRadios.map { it.uuid }.toSet())
    }

    private fun validCsvStream(): InputStream = """
        OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
        ISS (ZARYA),1998-067A,${LocalDate.now()}T12:28:09.322176,15.48582035,.0004694,51.6447,309.4881,203.6966,299.8876,0,U,25544,999,31220,.31985E-4,.1288E-4,0
    """.trimIndent().byteInputStream()

    private fun validTleStream(): InputStream = """
        ISS (ZARYA)
        1 25544U 98067A   $todayEpoch.51955234  .00001288  00000+0  31985-4 0  9990
        2 25544  51.6447 309.4881 0004694 203.6966 299.8876 15.48582035312205
    """.trimIndent().byteInputStream()

    private fun fresherTleStream(): InputStream = """
        ISS
        1 25544U 98067A   $todayEpoch.71955234  .00001288  00000+0  31985-4 0  9990
        2 25544  51.6447 309.4881 0004694 203.6966 299.8876 15.48582035312205
    """.trimIndent().byteInputStream()

    private fun twoRadiosStream(): InputStream = """
        [$radioAlive,{"uuid":"uuid-retired","description":"Retired","alive":true,"downlink_low":437800000,
        "mode":"FM","invert":false,"norad_cat_id":25544}]
    """.trimIndent().byteInputStream()

    private fun singleRadioStream(): InputStream = "[$radioAlive]".byteInputStream()

    private fun customRadioStream(): InputStream = """
        [{"uuid":"uuid-custom","description":"Local beacon","alive":true,"downlink_low":144800000,
        "mode":"FM","invert":false,"norad_cat_id":25544}]
    """.trimIndent().byteInputStream()
}

private const val radioAlive = """{"uuid":"uuid-alive","description":"Voice repeater","alive":true,
    "downlink_low":145800000,"mode":"FM","invert":false,"norad_cat_id":25544}"""

private class FakeRemoteSource : IRemoteSource {
    val fileStreams: MutableMap<String, () -> InputStream> = mutableMapOf()
    val networkStreams: MutableMap<String, () -> InputStream> = mutableMapOf()

    override suspend fun getFileStream(uri: String): InputStream? = fileStreams[uri]?.invoke()

    override suspend fun getNetworkStream(url: String): NetworkResult {
        val stream = networkStreams[url]?.invoke()
        return if (stream != null) NetworkResult(200, stream) else NetworkResult(404, null)
    }

    override suspend fun getAmSatCatalog(): String? = null

    override suspend fun getAmSatReports(hours: Int, limit: Int): String? = null

    override suspend fun submitAmSatReport(payloadJson: String): Pair<Int, String>? = null
}

private class FakeLocalSource : ILocalSource {
    val insertedEntries = mutableListOf<OrbitalData>()
    val insertedRadios = mutableListOf<SatRadio>()
    private val customRadios = mutableListOf<String>()

    override suspend fun getEntriesTotal(): Int = insertedEntries.size

    override suspend fun getEntriesList(): List<SatItem> = emptyList()

    override suspend fun getEntriesWithIds(ids: List<Int>): List<OrbitalObject> = emptyList()

    override suspend fun getEntriesEpochs(): Map<Int, Double> =
        insertedEntries.associate { entry -> entry.catnum to entry.epoch }

    override suspend fun getEntriesNames(): Map<Int, String> =
        insertedEntries.associate { entry -> entry.catnum to entry.name }

    override suspend fun renameEntries(names: Map<Int, String>) = names.forEach { (catnum, name) ->
        val index = insertedEntries.indexOfFirst { entry -> entry.catnum == catnum }
        if (index >= 0) insertedEntries[index] = insertedEntries[index].copy(name = name)
    }

    override suspend fun deleteEntriesWithIds(ids: List<Int>) {
        insertedEntries.removeAll { entry -> entry.catnum in ids }
    }

    override suspend fun insertEntries(entries: List<OrbitalData>) {
        insertedEntries += entries
    }

    override suspend fun deleteEntries() {
        insertedEntries.clear()
    }

    override suspend fun getIdsWithModes(modes: List<String>): List<Int> = emptyList()

    override suspend fun getRadiosTotal(): Int = insertedRadios.size

    override suspend fun getRadiosWithId(id: Int): List<SatRadio> = emptyList()

    override suspend fun insertRadios(radios: List<SatRadio>, isCustom: Boolean) {
        insertedRadios.removeAll { stored -> radios.any { radio -> radio.uuid == stored.uuid } }
        insertedRadios += radios
        if (isCustom) customRadios += radios.map { radio -> radio.uuid }
        else customRadios -= radios.map { radio -> radio.uuid }.toSet()
    }

    override suspend fun deleteRadios() {
        insertedRadios.clear()
        customRadios.clear()
    }

    override suspend fun deleteManagedRadios() {
        insertedRadios.removeAll { radio -> radio.uuid !in customRadios }
    }
}

private class FakeSettingsRepo(dataSources: DataSourcesSettings = defaultDataSourcesSettings()) : ISettingsRepo {

    override val appVersionName: String = "test"
    override val appVersionCode: Long = 1L

    override val selectedIds: StateFlow<List<Int>> = MutableStateFlow(emptyList())

    override val selectedSatModes: StateFlow<List<String>> = MutableStateFlow(emptyList())

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

    override val dataSourcesSettings: MutableStateFlow<DataSourcesSettings> = MutableStateFlow(dataSources)

    override val dataSourcesStatus: MutableStateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())

    override val radioControlSettings: StateFlow<RadioControlSettings> = MutableStateFlow(
        RadioControlSettings(false, RadioControlSettings.MODEL_YAESU_FT817, "", "", "", "", 9600)
    )

    override fun setSelectedIds(ids: List<Int>) = Unit

    override fun setSelectedSatModes(modes: List<String>) = Unit

    override fun setPassesSettings(settings: PassesSettings) = Unit

    override fun setStationPosition(latitude: Double, longitude: Double, altitude: Double): Boolean = true

    override fun setStationPosition(): Boolean = true

    override fun setStationPosition(locator: String): Boolean = true


    override fun updateDatabaseState(state: DatabaseState) {
        databaseState.value = state
    }

    override fun updateRCSettings(settings: RCSettings) = Unit

    override fun updateOtherSettings(transform: (OtherSettings) -> OtherSettings) = Unit

    override fun updateDataSourcesSettings(settings: DataSourcesSettings) {
        dataSourcesSettings.value = settings
    }

    override fun updateDataSourcesStatus(status: Map<String, Int>) {
        dataSourcesStatus.value = status
    }

    override fun updateRadioControlSettings(settings: RadioControlSettings) = Unit

    override fun getSatelliteOffset(catnum: Int): String = ""

    override fun setSatelliteOffset(catnum: Int, offset: String) = Unit

    override fun getAmSatCallsign(): String = ""

    override fun setAmSatCallsign(callsign: String) = Unit
}

private fun defaultDataSourcesSettings(): DataSourcesSettings {
    return DataSourcesSettings(
        satelliteUrls = emptyList(),
        transceiversUrls = emptyList()
    )
}
