package com.rtbishop.lookingsat.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rtbishop.lookingsat.Injector
import com.rtbishop.lookingsat.repo.Repository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: Repository = Injector.provideRepository(application)
}