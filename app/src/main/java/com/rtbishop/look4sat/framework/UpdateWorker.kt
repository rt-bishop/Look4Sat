package com.rtbishop.look4sat.framework

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rtbishop.look4sat.presentation.MainApplication
import java.text.DateFormat
import java.util.*

class UpdateWorker constructor(private val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val dateTime = DateFormat.getDateTimeInstance().format(Calendar.getInstance().time)
        Log.d("UpdateWorker", "Started periodic data update on $dateTime")
        (appContext.applicationContext as MainApplication).repository.updateFromWeb()
        return Result.success()
    }
}
