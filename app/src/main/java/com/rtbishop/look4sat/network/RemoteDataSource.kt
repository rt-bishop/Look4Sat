/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.network

import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.data.Transmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.*
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val api: TransmittersApi,
    private val client: OkHttpClient
) : RemoteSource {

    override suspend fun fetchTleStream(urlList: List<TleSource>): InputStream {
        val streamTable = Hashtable<String, InputStream>()
        withContext(Dispatchers.IO) {
            urlList.forEach {
                val request = Request.Builder().url(it.url).build()
                val stream = client.newCall(request).execute().body?.byteStream()
                streamTable[it.url] = stream
            }
        }
        return SequenceInputStream(streamTable.elements())
    }

    override suspend fun fetchTransmitters(): List<Transmitter> {
        return api.fetchTransmitters()
    }
}
