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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainApplication : Application() {

    lateinit var container: MainContainer

    override fun onCreate() {
        disableMobileLandscapeLayout()
        super.onCreate()
        container = MainContainer(this)
        // trigger automatic update every 48 hours
        container.appScope.launch { checkAutoUpdate() }
        // load satellite data on every app start
        container.appScope.launch { container.satelliteRepo.initRepository() }
    }

    private suspend fun checkAutoUpdate(timeNow: Long = System.currentTimeMillis()) {
        if (container.settingsRepo.otherSettings.value.stateOfAutoUpdate) {
            val timeDelta = timeNow - container.settingsRepo.databaseState.value.updateTimestamp
            if (timeDelta > 172_800_000L) { // 48 hours in ms
                val sdf = SimpleDateFormat("d MMM yyyy - HH:mm:ss", Locale.getDefault())
                println("Started periodic data update on ${sdf.format(Date())}")
                container.databaseRepo.updateFromRemote()
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun disableMobileLandscapeLayout() {
        val isTablet = applicationContext.resources?.getBoolean(R.bool.isTablet) ?: true
        val lifecycleAdapter = object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (!isTablet) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        registerActivityLifecycleCallbacks(lifecycleAdapter)
    }
}
