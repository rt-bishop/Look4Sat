package com.rtbishop.look4sat.dagger.modules

import android.content.ContentResolver
import android.content.Context
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
    fun provideContentResolver(context: Context): ContentResolver {
        return context.applicationContext.contentResolver
    }

    @Singleton
    @Provides
    fun provideDefaultRepository(
        resolver: ContentResolver,
        localSource: LocalSource,
        remoteSource: RemoteSource
    ): Repository {
        return DefaultRepository(resolver, localSource, remoteSource)
    }
}
