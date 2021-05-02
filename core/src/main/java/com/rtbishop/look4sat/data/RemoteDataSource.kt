package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.model.SatTrans
import java.io.InputStream

interface RemoteDataSource {

    suspend fun fetchDataStream(url: String): InputStream?

    suspend fun fetchTransmitters(): List<SatTrans>
}
