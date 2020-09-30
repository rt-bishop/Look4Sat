package com.rtbishop.look4sat.repo

import android.net.Uri
import androidx.lifecycle.LiveData
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.data.TleSource

interface Repository {

    fun getSources(): LiveData<List<TleSource>>

    suspend fun updateSources(sources: List<TleSource>)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun getEntries(): LiveData<List<SatEntry>>

    suspend fun updateEntriesFromFile(fileUri: Uri)

    suspend fun updateEntriesFromSources(sources: List<TleSource>)

    suspend fun updateEntriesSelection(satIds: List<Int>)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun updateTransmitters()

    suspend fun getTransmittersForSatId(satId: Int): List<SatTrans>
}
