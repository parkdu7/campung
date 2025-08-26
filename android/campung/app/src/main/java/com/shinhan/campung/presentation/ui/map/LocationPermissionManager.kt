package com.shinhan.campung.presentation.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

class LocationPermissionManager(private val context: Context) {
    
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
    
    fun requestLocationPermission(
        permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    ) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    fun handlePermissionResult(
        result: Map<String, Boolean>,
        onPermissionGranted: () -> Unit
    ) {
        val hasPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (hasPermission) {
            onPermissionGranted()
        }
    }
}