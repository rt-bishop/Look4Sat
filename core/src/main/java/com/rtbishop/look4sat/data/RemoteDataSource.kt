package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.model.Transmitter
import java.io.InputStream

interface RemoteDataSource {

    fun fetchFileStream(url: String): InputStream

    fun fetchTransmitters(): List<Transmitter>
}
