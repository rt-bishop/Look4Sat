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
package com.rtbishop.look4sat.core.data.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.rtbishop.look4sat.core.domain.usecase.ISaveImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Legacy public storage writes require WRITE_EXTERNAL_STORAGE on API 24-28.
class SaveImage(private val context: Context) : ISaveImage {

    override suspend fun invoke(pixels: IntArray, width: Int, height: Int, modeName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                    val filename = "SSTV_${modeName}_$timestamp.png"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveWithMediaStore(bitmap, filename)
                    } else {
                        saveToExternalStorage(bitmap, filename)
                    }
                    true
                } finally {
                    bitmap.recycle()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SaveImage", "Failed to save SSTV image", e)
                false
            }
        }
    }

    private fun saveWithMediaStore(bitmap: Bitmap, filename: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Look4Sat")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create MediaStore entry")
        var saved = false
        try {
            val stream = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Failed to open MediaStore output stream")
            stream.use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Failed to encode PNG" }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1) { "Failed to publish image" }
            saved = true
        } finally {
            if (!saved) {
                check(resolver.delete(uri, null, null) == 1) { "Failed to delete incomplete image" }
            }
        }
    }

    private fun saveToExternalStorage(bitmap: Bitmap, filename: String) {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(picturesDir, "Look4Sat")
        check(dir.isDirectory || dir.mkdirs()) { "Failed to create pictures directory" }
        val file = File(dir, filename)
        var saved = false
        try {
            FileOutputStream(file).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Failed to encode PNG" }
            }
            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf("image/png"), null
            )
            saved = true
        } finally {
            if (!saved && file.exists()) {
                check(file.delete()) { "Failed to delete incomplete image" }
            }
        }
    }
}
