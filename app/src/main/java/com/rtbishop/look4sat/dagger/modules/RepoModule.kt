package com.rtbishop.look4sat.dagger.modules

import com.rtbishop.look4sat.network.RemoteSource
import com.rtbishop.look4sat.persistence.LocalSource
import com.rtbishop.look4sat.repo.DefaultRepository
import com.rtbishop.look4sat.repo.Repository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule {

    @Singleton
    @Provides
    fun provideDefaultRepository(localSource: LocalSource, remoteSource: RemoteSource): Repository {
        return DefaultRepository(localSource, remoteSource)
    }
}
