package com.rtbishop.lookingsat

import android.app.Application
import com.rtbishop.lookingsat.di.AppComponent
import com.rtbishop.lookingsat.di.DaggerAppComponent

class LookingSatApp : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
}