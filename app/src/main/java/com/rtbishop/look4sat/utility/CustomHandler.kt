package com.rtbishop.look4sat.utility

import android.app.Activity

class CustomHandler(private val activity: Activity) : Thread.UncaughtExceptionHandler {

    private val rootHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
//         Custom exception handling, use intents instead of dialogs
//         Process.killProcess(Process.myPid())
//         exitProcess(0)
    }
}
