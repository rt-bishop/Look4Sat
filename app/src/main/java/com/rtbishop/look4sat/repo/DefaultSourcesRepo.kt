package com.rtbishop.look4sat.repo

import androidx.lifecycle.LiveData
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.repo.local.SourcesDao
import javax.inject.Inject

class DefaultSourcesRepo @Inject constructor(private val sourcesDao: SourcesDao) :
    SourcesRepo {

    override fun getSources(): LiveData<List<TleSource>> {
        return sourcesDao.getSources()
    }

    override suspend fun updateSources(sources: List<TleSource>) {
        sourcesDao.updateSources(sources)
    }
}