package com.shinhan.campung.di

import android.content.Context
import com.shinhan.campung.data.service.fcm.NotificationHandler
import com.shinhan.campung.data.service.fcm.NotificationRouter
import com.shinhan.campung.data.service.fcm.handlers.GeneralNotificationHandler
import com.shinhan.campung.data.service.fcm.handlers.LocationShareNotificationHandler
import com.shinhan.campung.data.service.fcm.handlers.PostNotificationHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * FCM 알림 처리를 위한 DI 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @IntoSet
    fun providePostNotificationHandler(
        @ApplicationContext context: Context
    ): NotificationHandler = PostNotificationHandler(context)
    
    @Provides
    @IntoSet
    fun provideLocationShareNotificationHandler(
        @ApplicationContext context: Context
    ): NotificationHandler = LocationShareNotificationHandler(context)
    
    @Provides
    @IntoSet
    fun provideGeneralNotificationHandler(
        @ApplicationContext context: Context
    ): NotificationHandler = GeneralNotificationHandler(context)
    
    @Provides
    @Singleton
    fun provideNotificationRouter(
        handlers: Set<@JvmSuppressWildcards NotificationHandler>
    ): NotificationRouter = NotificationRouter(handlers)
}