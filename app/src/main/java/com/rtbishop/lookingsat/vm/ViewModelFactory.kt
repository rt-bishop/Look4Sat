package com.rtbishop.lookingsat.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rtbishop.lookingsat.repo.Repository

class ViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}