/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.data.source

import android.content.ContentResolver
import androidx.core.net.toUri
import com.rtbishop.look4sat.core.domain.source.IRemoteSource
import com.rtbishop.look4sat.core.domain.source.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

class RemoteSource(
    private val dispatcher: CoroutineDispatcher,
    private val contentResolver: ContentResolver,
    private val httpClient: OkHttpClient
) : IRemoteSource {

    override suspend fun getFileStream(uri: String): InputStream? = withContext(dispatcher) {
        try {
            val fileUri = uri.toUri()
            contentResolver.openInputStream(fileUri)?.buffered()
        } catch (exception: Exception) {
            println("RemoteSource file stream exception: $exception")
            null
        }
    }

    override suspend fun getNetworkStream(url: String): NetworkResult = withContext(dispatcher) {
        try {
            val networkRequest = Request.Builder().url(url).build()
            val response = httpClient.newCall(networkRequest).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                return@withContext NetworkResult(code, null)
            }
            // Return the body stream directly as the caller is responsible for closing it
            // That returns the connection to OkHttp's pool
            val body = response.body
            if (body == null) {
                response.close()
                return@withContext NetworkResult(response.code, null)
            }
            NetworkResult(response.code, body.byteStream().buffered())
        } catch (exception: Exception) {
            println("RemoteSource network stream exception: $exception")
            NetworkResult(NetworkResult.CONNECTION_ERROR, null)
        }
    }

    override suspend fun getAmSatCatalog(): String? = withContext(dispatcher) {
        try {
            val request = Request.Builder()
                .url("https://www.amsat.org/status/api/v1/catalog.php")
                .header("User-Agent", "Look4Sat")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.string()
            }
        } catch (exception: Exception) {
            println("RemoteSource getAmSatCatalog exception: $exception")
            null
        }
    }

    override suspend fun getAmSatReports(hours: Int, limit: Int): String? = withContext(dispatcher) {
        try {
            val request = Request.Builder()
                .url("https://www.amsat.org/status/api/v1/reports.php?hours=$hours&limit=$limit")
                .header("User-Agent", "Look4Sat")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.string()
            }
        } catch (exception: Exception) {
            println("RemoteSource getAmSatReports exception: $exception")
            null
        }
    }

    override suspend fun submitAmSatReport(payloadJson: String): Pair<Int, String>? = withContext(dispatcher) {
        try {
            val body = payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://www.amsat.org/status/api/v1/reports.php")
                .header("User-Agent", "Look4Sat")
                .post(body)
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.code to response.body.string()
            }
        } catch (exception: Exception) {
            println("RemoteSource submitAmSatReport exception: $exception")
            null
        }
    }
}
