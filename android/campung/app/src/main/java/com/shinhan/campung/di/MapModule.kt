package com.shinhan.campung.di

import com.shinhan.campung.data.remote.api.MapApiService
import com.shinhan.campung.data.repository.MapContentRepository
import com.shinhan.campung.data.repository.MapContentRepositoryImpl
import com.shinhan.campung.data.repository.MockMapContentRepository
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
    
    // 실제 API 사용시
    // @Binds
    // abstract fun bindMapContentRepository(
    //     mapContentRepositoryImpl: MapContentRepositoryImpl
    // ): MapContentRepository
    
    // 테스트용 Mock Repository 사용
    @Binds
    abstract fun bindMapContentRepository(
        mockMapContentRepository: MockMapContentRepository
    ): MapContentRepository
}

@Module
@InstallIn(SingletonComponent::class)
object MapApiModule {
    
    @Provides
    @Singleton
    fun provideMapApiService(retrofit: Retrofit): MapApiService {
        return retrofit.create(MapApiService::class.java)
    }
}