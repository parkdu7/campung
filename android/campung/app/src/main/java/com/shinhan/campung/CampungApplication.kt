package com.shinhan.campung

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampungApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}