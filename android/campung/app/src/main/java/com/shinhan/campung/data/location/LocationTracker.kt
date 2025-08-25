package com.shinhan.campung.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.shinhan.campung.util.GeohashUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTracker @Inject constructor(
    private val context: Context
) : LocationListener {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var isTracking = false
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation
    
    private val _currentGeohash = MutableStateFlow<String?>(null)
    val currentGeohash: StateFlow<String?> = _currentGeohash
    
    fun startTracking() {
        if (isTracking) return
        
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return
        }
        
        try {
            // GPS와 네트워크 위치 모두 사용
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    this
                )
            }
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    this
                )
            }
            
            // 마지막으로 알려진 위치 가져오기
            getLastKnownLocation()?.let { location ->
                Log.d(TAG, "Last known location: lat=${location.latitude}, lon=${location.longitude}")
                updateLocation(location)
            } ?: Log.w(TAG, "No last known location available")
            
            isTracking = true
            Log.d(TAG, "Location tracking started")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
        }
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        try {
            locationManager.removeUpdates(this)
            isTracking = false
            Log.d(TAG, "Location tracking stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when removing location updates", e)
        }
    }
    
    override fun onLocationChanged(location: Location) {
        updateLocation(location)
    }
    
    private fun updateLocation(location: Location) {
        _currentLocation.value = location
        
        // 현재 위치 로그 추가
        Log.d(TAG, "Current Location: lat=${location.latitude}, lon=${location.longitude}")
        
        val newGeohash = GeohashUtil.encode(location.latitude, location.longitude)
        val oldGeohash = _currentGeohash.value
        
        Log.d(TAG, "Geohash: $newGeohash")
        
        if (GeohashUtil.isSignificantMove(oldGeohash, newGeohash)) {
            _currentGeohash.value = newGeohash
            Log.d(TAG, "Location changed: $oldGeohash -> $newGeohash")
        }
    }
    
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            return when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting last known location", e)
            return null
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    companion object {
        private const val TAG = "LocationTracker"
        private const val MIN_TIME_BETWEEN_UPDATES = 30000L // 30초
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10미터
    }
}