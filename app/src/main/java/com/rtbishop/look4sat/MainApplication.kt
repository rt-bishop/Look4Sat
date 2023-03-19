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
package com.rtbishop.look4sat

import android.app.Application
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISettingsRepository
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var dataRepository: IDataRepository

    @Inject
    lateinit var settingsRepository: ISettingsRepository

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        checkAutoUpdate()
    }

    private fun checkAutoUpdate() {
        if (settingsRepository.isUpdateEnabled()) {
            val timeDelta = System.currentTimeMillis() - settingsRepository.getLastUpdateTime()
            if (timeDelta > 172800000) { // 48 hours in ms
                val sdf = SimpleDateFormat("d MMM yyyy - HH:mm:ss", Locale.getDefault())
                Timber.d("Started periodic data update on ${sdf.format(Date())}")
                dataRepository.updateFromWeb()
            }
        }
    }
}
