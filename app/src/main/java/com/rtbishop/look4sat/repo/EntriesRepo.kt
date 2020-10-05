package com.rtbishop.look4sat.repo

import android.net.Uri
import androidx.lifecycle.LiveData
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.TleSource

interface EntriesRepo {

    fun getEntries(): LiveData<List<SatEntry>>

    suspend fun updateEntriesFromFile(fileUri: Uri)

    suspend fun updateEntriesFromSources(sources: List<TleSource>)

    suspend fun updateEntriesSelection(satIds: List<Int>)
}