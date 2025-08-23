package com.shinhan.campung.di

import android.content.Context
import androidx.room.Room
import com.shinhan.campung.data.local.CampungDatabase
import com.shinhan.campung.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//@Module  // 임시 비활성화
//@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Room 임시 비활성화 - 크래시 원인 확인용
    /*
    @Provides
    @Singleton
    fun provideCampungDatabase(
        @ApplicationContext context: Context
    ): CampungDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            CampungDatabase::class.java,
            "campung_database"
        ).build()
    }

    @Provides
    fun provideUserDao(database: CampungDatabase): UserDao {
        return database.userDao()
    }
    */
}