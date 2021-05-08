package com.rtbishop.look4sat.framework.api

import com.rtbishop.look4sat.data.SatDataRemoteSource
import com.rtbishop.look4sat.domain.model.SatTrans
import com.rtbishop.look4sat.utility.DataMapper
import java.io.InputStream

class SatDataRemote(private val api: SatDataService) : SatDataRemoteSource {

    override suspend fun fetchDataStream(url: String): InputStream? {
        return api.fetchFileByUrl(url).body()?.byteStream()
    }

    override suspend fun fetchTransmitters(): List<SatTrans> {
        return DataMapper.satTransListToDomainTransList(api.fetchTransmitters())
    }
}
