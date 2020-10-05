package com.rtbishop.look4sat.repo

import androidx.lifecycle.LiveData
import com.rtbishop.look4sat.data.TleSource

interface SourcesRepo {

    fun getSources(): LiveData<List<TleSource>>

    suspend fun updateSources(sources: List<TleSource>)
}