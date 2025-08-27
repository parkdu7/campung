package com.shinhan.campung.di

import com.shinhan.campung.data.remote.api.ContentApiService
import com.shinhan.campung.data.remote.api.MapApiService
import com.shinhan.campung.data.repository.ContentRepository
import com.shinhan.campung.data.repository.ContentRepositoryImpl
import com.shinhan.campung.data.repository.MapContentRepository
import com.shinhan.campung.data.repository.MapContentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {
    
    // 실제 API 사용
    @Binds
    abstract fun bindMapContentRepository(
        mapContentRepositoryImpl: MapContentRepositoryImpl
    ): MapContentRepository
    
    @Binds
    abstract fun bindContentRepository(
        contentRepositoryImpl: ContentRepositoryImpl
    ): ContentRepository
}

@Module
@InstallIn(SingletonComponent::class)
object MapApiModule {
    
    @Provides
    @Singleton
    fun provideMapApiService(retrofit: Retrofit): MapApiService {
        return retrofit.create(MapApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideContentApiService(retrofit: Retrofit): ContentApiService {
        return retrofit.create(ContentApiService::class.java)
    }
}