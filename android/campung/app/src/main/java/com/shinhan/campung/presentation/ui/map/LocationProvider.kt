package com.shinhan.campung.presentation.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng

class LocationProvider(private val context: Context) {
    
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    @SuppressLint("MissingPermission")
    fun fetchMyLocationOnce(
        hasLocationPermission: Boolean,
        onLocationReceived: (LatLng?) -> Unit
    ) {
        if (!hasLocationPermission) {
            onLocationReceived(null)
            return
        }
        
        // Last location first
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onLocationReceived(LatLng(loc.latitude, loc.longitude))
                return@addOnSuccessListener
            }
        }
        
        // Current location with proper priority
        val cts = CancellationTokenSource()
        val priority = if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        fused.getCurrentLocation(priority, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocationReceived(LatLng(loc.latitude, loc.longitude))
                } else {
                    onLocationReceived(null)
                }
            }
            .addOnFailureListener {
                onLocationReceived(null)
            }
    }
}