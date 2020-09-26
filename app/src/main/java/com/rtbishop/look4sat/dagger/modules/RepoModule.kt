package com.rtbishop.look4sat.dagger.modules

import android.content.ContentResolver
import android.content.Context
import com.rtbishop.look4sat.repo.DefaultRepository
import com.rtbishop.look4sat.repo.Repository
import com.rtbishop.look4sat.repo.local.EntriesDao
import com.rtbishop.look4sat.repo.local.SourcesDao
import com.rtbishop.look4sat.repo.local.TransDao
import com.rtbishop.look4sat.repo.remote.TransApi
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
        client: OkHttpClient,
        transApi: TransApi,
        entriesDao: EntriesDao,
        transDao: TransDao,
        sourcesDao: SourcesDao
    ): Repository {
        return DefaultRepository(
            resolver,
            client,
            transApi,
            entriesDao,
            sourcesDao,
            transDao
        )
    }
}
