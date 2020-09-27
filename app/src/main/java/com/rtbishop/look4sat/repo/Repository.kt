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

    fun getAllEntries(): LiveData<List<SatEntry>>

    fun getSelectedEntries(): LiveData<List<SatEntry>>

    suspend fun updateEntriesFromFile(tleUri: Uri)

    suspend fun updateEntriesFromUrl(urlList: List<TleSource>)

    suspend fun updateEntriesSelection(catNumList: List<Int>)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun updateTransmitters()

    suspend fun getTransmittersByCatNum(catNum: Int): List<SatTrans>
}
