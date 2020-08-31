package com.rtbishop.look4sat.network

import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.data.Transmitter
import java.io.InputStream

interface RemoteSource {

    suspend fun fetchTleStreams(urlList: List<TleSource>): List<InputStream>

    suspend fun fetchTransmitters(): List<Transmitter>
}
