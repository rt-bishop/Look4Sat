package com.rtbishop.lookingsat.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rtbishop.lookingsat.repo.Transmitter

class TransmitterService(private val transmitterApi: TransmitterApi) {
    private val _transmittersList = MutableLiveData<List<Transmitter>>()
    val transmittersList: LiveData<List<Transmitter>> = _transmittersList

    suspend fun fetchTransmitters() {
        _transmittersList.postValue(transmitterApi.getTransmitterList())
    }
}