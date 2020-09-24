package com.rtbishop.look4sat.dagger.modules

import android.content.ContentResolver
import android.content.Context
import com.rtbishop.look4sat.repo.DefaultRepository
import com.rtbishop.look4sat.repo.Repository
import com.rtbishop.look4sat.repo.api.TransmittersApi
import com.rtbishop.look4sat.repo.dao.EntriesDao
import com.rtbishop.look4sat.repo.dao.SourcesDao
import com.rtbishop.look4sat.repo.dao.TransmittersDao
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
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
        transmittersApi: TransmittersApi,
        client: OkHttpClient,
        entriesDao: EntriesDao,
        transmittersDao: TransmittersDao,
        sourcesDao: SourcesDao
    ): Repository {
        return DefaultRepository(
            resolver,
            client,
            transmittersApi,
            entriesDao,
            sourcesDao,
            transmittersDao
        )
    }
}
