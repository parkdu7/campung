package com.shinhan.campung.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.location.LocationTracker
import com.shinhan.campung.data.remote.api.AuthApi
import com.shinhan.campung.data.remote.api.MapApi
import com.shinhan.campung.data.repository.AuthRepository
import com.shinhan.campung.data.repository.MapRepository
import com.shinhan.campung.data.repository.NewPostRepository
import com.shinhan.campung.data.websocket.WebSocketService
import com.shinhan.campung.util.Constants
import android.util.Log
import com.shinhan.campung.data.remote.api.FriendApi
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
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
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
}

//import com.google.gson.Gson
//import com.google.gson.GsonBuilder
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.util.concurrent.TimeUnit
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object NetworkModule {
//
//    private const val BASE_URL = "https://api.example.com/"
//
//    @Provides
//    @Singleton
//    fun provideGson(): Gson {
//        return GsonBuilder()
//            .create()
//    }
//
//    @Provides
//    @Singleton
//    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
//        return HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//    }
//
//    @Provides
//    @Singleton
//    fun provideOkHttpClient(
//        httpLoggingInterceptor: HttpLoggingInterceptor
//    ): OkHttpClient {
//        return OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .addInterceptor(httpLoggingInterceptor)
//            .build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideRetrofit(
//        okHttpClient: OkHttpClient,
//        gson: Gson
//    ): Retrofit {
//        return Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .build()
//    }
//}