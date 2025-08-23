package com.shinhan.campung.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shinhan.campung.data.local.dao.UserDao
import com.shinhan.campung.data.local.entity.User

@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class CampungDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}