package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.data.source.AmSatParser
import com.rtbishop.look4sat.core.domain.model.SatStatusPage
import com.rtbishop.look4sat.core.domain.repository.IAmSatRepository
import com.rtbishop.look4sat.core.domain.source.IRemoteSource

/** AMSAT 状态仓库: 抓 HTML → 解析 → SatStatusPage */
class AmSatRepository(private val remoteSource: IRemoteSource) : IAmSatRepository {

    override suspend fun fetchStatus(): SatStatusPage? {
        val html = remoteSource.getStatusHtml() ?: return null
        if (html.isBlank()) return null
        return try {
            AmSatParser.parse(html, System.currentTimeMillis())
        } catch (exception: Exception) {
            println("AmSatRepository parse exception: $exception")
            null
        }
    }
}
