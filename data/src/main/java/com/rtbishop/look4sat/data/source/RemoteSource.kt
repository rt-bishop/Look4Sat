/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.data.source

import android.content.ContentResolver
import android.net.Uri
import com.rtbishop.look4sat.domain.source.IRemoteSource
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RemoteSource(
    private val contentResolver: ContentResolver,
    private val httpClient: OkHttpClient,
    private val dispatcher: CoroutineDispatcher
) : IRemoteSource {

    override suspend fun getFileStream(uri: String): InputStream? = withContext(dispatcher) {
        try {
            val fileUri = Uri.parse(uri)
            contentResolver.openInputStream(fileUri)
        } catch (exception: Exception) {
            println("RemoteSource file stream exception: $exception")
            null
        }
    }

    override suspend fun getNetworkStream(url: String): InputStream? = withContext(dispatcher) {
        try {
            val networkRequest = Request.Builder().url(url).build()
            httpClient.newCall(networkRequest).execute().body?.byteStream()
        } catch (exception: Exception) {
            println("RemoteSource network stream exception: $exception")
            null
        }
    }
}
