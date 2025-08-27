package com.shinhan.campung.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.location.LocationTracker
import com.shinhan.campung.data.remote.interceptor.AuthInterceptor
import com.shinhan.campung.data.remote.api.AuthApi
import com.shinhan.campung.data.remote.api.MapApi
import com.shinhan.campung.data.repository.AuthRepository
import com.shinhan.campung.data.repository.MapRepository
import com.shinhan.campung.data.repository.NewPostRepository
import com.shinhan.campung.data.websocket.WebSocketService
import com.shinhan.campung.util.Constants
import android.util.Log
import com.shinhan.campung.data.remote.api.ContentsApiService
import com.shinhan.campung.data.remote.api.FriendApi
import com.shinhan.campung.data.remote.api.LocationApi
import com.shinhan.campung.data.remote.api.NotificationApi
import com.shinhan.campung.data.repository.ContentsRepository
import com.shinhan.campung.data.repository.FriendRepository
import com.shinhan.campung.data.repository.LocationRepository
import com.shinhan.campung.data.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides @Singleton
    fun provideAuthInterceptor(authDataStore: AuthDataStore): AuthInterceptor =
        AuthInterceptor(authDataStore)

    @Provides @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides @Singleton
    fun provideRetrofit(gson: Gson, client: OkHttpClient): Retrofit {
        Log.d("NetworkModule", "BASE_URL: ${Constants.BASE_URL}")
        return Retrofit.Builder()
            .baseUrl("https://campung.my/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
    }

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideMapApi(retrofit: Retrofit): MapApi =
        retrofit.create(MapApi::class.java)

    @Provides @Singleton
    fun provideFriendApi(retrofit: Retrofit): FriendApi =
        retrofit.create(FriendApi::class.java)

    @Provides @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)

    @Provides @Singleton
    fun provideLocationApi(retrofit: Retrofit): LocationApi =
        retrofit.create(LocationApi::class.java)

    @Provides @Singleton
    fun provideAuthDataStore(@ApplicationContext context: Context) = AuthDataStore(context)

    @Provides @Singleton
    fun provideAuthRepository(api: AuthApi, ds: AuthDataStore) = AuthRepository(api, ds)

    @Provides @Singleton
    fun provideMapRepository(api: MapApi) = MapRepository(api)
    
    @Provides @Singleton
    fun provideWebSocketService(gson: Gson) = WebSocketService(gson)
    
    @Provides @Singleton
    fun provideLocationTracker(@ApplicationContext context: Context) = LocationTracker(context)
    
    @Provides @Singleton
    fun provideNewPostRepository(
        webSocketService: WebSocketService,
        locationTracker: LocationTracker,
        authDataStore: AuthDataStore
    ) = NewPostRepository(webSocketService, locationTracker, authDataStore)
    
    @Provides @Singleton
    fun provideNotificationRepository(
        notificationApi: NotificationApi,
        authDataStore: AuthDataStore
    ) = NotificationRepository(notificationApi, authDataStore)
    
    @Provides @Singleton
    fun provideFriendRepository(
        friendApi: FriendApi,
        authDataStore: AuthDataStore
    ) = FriendRepository(friendApi, authDataStore)
    
    @Provides @Singleton
    fun provideLocationRepository(
        locationApi: LocationApi,
        authDataStore: AuthDataStore
    ) = LocationRepository(locationApi, authDataStore)

    @Provides @Singleton
    fun provideContentsApi(retrofit: Retrofit): ContentsApiService =
        retrofit.create(ContentsApiService::class.java)

    @Provides @Singleton
    fun provideContentsRepository(
        api: ContentsApiService
    ) = ContentsRepository(api)
}