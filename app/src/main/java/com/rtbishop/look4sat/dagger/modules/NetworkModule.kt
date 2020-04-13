/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.dagger.modules

import com.rtbishop.look4sat.network.RemoteDataSource
import com.rtbishop.look4sat.network.RemoteSource
import com.rtbishop.look4sat.network.TransmittersApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Singleton
    @Provides
    fun provideRemoteDataSource(api: TransmittersApi, client: OkHttpClient): RemoteSource {
        return RemoteDataSource(api, client)
    }

    @Singleton
    @Provides
    fun provideTransmittersApi(): TransmittersApi {
        return Retrofit.Builder()
            .baseUrl("https://db.satnogs.org/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TransmittersApi::class.java)
    }
}
