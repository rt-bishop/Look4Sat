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
package com.rtbishop.look4sat.framework

import android.content.Context
import android.util.Log
import androidx.work.*
import com.rtbishop.look4sat.presentation.MainApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(@ApplicationContext context: Context) {

    private val workTag = "AutoUpdateWork"
    private val workManager = WorkManager.getInstance(context)

    fun toggleAutoUpdate(isEnabled: Boolean) {
        if (isEnabled) enableAutoUpdate() else disableAutoUpdate()
        Log.d("UpdateManager", workManager.getWorkInfosForUniqueWork(workTag).get().toString())
    }

    private fun enableAutoUpdate() {
        val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequest.Builder(UpdateWorker::class.java, 24, TimeUnit.HOURS)
            .setConstraints(network).build()
        workManager.enqueueUniquePeriodicWork(workTag, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    private fun disableAutoUpdate() {
        workManager.cancelUniqueWork(workTag)
    }

    class UpdateWorker constructor(private val context: Context, params: WorkerParameters) :
        Worker(context, params) {

        override fun doWork(): Result {
            val dateTime = DateFormat.getDateTimeInstance().format(Calendar.getInstance().time)
            Log.d("UpdateWorker", "Started periodic data update on $dateTime")
            (context.applicationContext as MainApplication).repository.updateFromWeb()
            return Result.success()
        }
    }
}
