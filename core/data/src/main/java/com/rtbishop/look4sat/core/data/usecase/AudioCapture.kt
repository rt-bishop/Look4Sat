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

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.rtbishop.look4sat.core.domain.usecase.IAudioCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AudioCapture : IAudioCapture {

    override val sampleRate: Int = 44100

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        .coerceAtLeast(sampleRate) // at least 1 second buffer

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun audioFlow(): Flow<FloatArray> = flow {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 4 // bytes for float
        )
        try {
            recorder.startRecording()
            val chunkSize = sampleRate / 10 // ~100ms chunks
            val buffer = FloatArray(chunkSize)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, chunkSize, AudioRecord.READ_BLOCKING)
                if (read > 0) emit(if (read == chunkSize) buffer.copyOf() else buffer.copyOfRange(0, read))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
