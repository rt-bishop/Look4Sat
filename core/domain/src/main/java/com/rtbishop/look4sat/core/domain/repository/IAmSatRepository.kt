package com.rtbishop.look4sat.core.domain.repository

import com.rtbishop.look4sat.core.domain.model.SatStatusPage

/** AMSAT satellite status data source */
interface IAmSatRepository {
    /** Fetch and parse the AMSAT status page; null on failure */
    suspend fun fetchStatus(): SatStatusPage?
}
