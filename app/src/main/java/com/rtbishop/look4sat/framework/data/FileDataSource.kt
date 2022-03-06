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
package com.rtbishop.look4sat.framework.data

import android.content.ContentResolver
import android.net.Uri
import com.rtbishop.look4sat.domain.data.IFileDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream

class FileDataSource(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher
) : IFileDataSource {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getDataStream(uri: String): InputStream? {
        return withContext(ioDispatcher) { contentResolver.openInputStream(Uri.parse(uri)) }
    }
}
