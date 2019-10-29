package com.rtbishop.lookingsat.repo

import com.rtbishop.lookingsat.api.RemoteDataSource
import com.rtbishop.lookingsat.db.LocalDataSource

class Repository(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource
)