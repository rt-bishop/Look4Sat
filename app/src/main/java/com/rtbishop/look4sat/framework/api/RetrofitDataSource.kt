package com.rtbishop.look4sat.framework.api

import com.rtbishop.look4sat.data.RemoteDataSource
import com.rtbishop.look4sat.domain.model.SatTrans
import java.io.InputStream

class RetrofitDataSource(private val satelliteService: SatelliteService) : RemoteDataSource {

    override suspend fun fetchFileStream(url: String): InputStream? {
        return satelliteService.fetchFileByUrl(url).body()?.byteStream()
    }

    override suspend fun fetchTransmitters(): List<SatTrans> {
        return satelliteService.fetchTransmitters().map { trans ->
            SatTrans(
                trans.uuid, trans.info, trans.isAlive, trans.downlink,
                trans.uplink, trans.mode, trans.isInverted, trans.catNum
            )
        }
    }
}
