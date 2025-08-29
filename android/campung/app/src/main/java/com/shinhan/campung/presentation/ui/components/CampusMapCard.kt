package com.shinhan.campung.presentation.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import com.shinhan.campung.presentation.ui.map.MapClusterManager
import com.shinhan.campung.presentation.ui.components.MyLocationMarker
import com.shinhan.campung.presentation.ui.components.LocationMarkerManager

private val LatLngSaver: Saver<LatLng?, String> = Saver(
    save = { loc -> loc?.let { "${it.latitude},${it.longitude}" } ?: "" },
    restore = { s ->
        if (s.isEmpty()) null
        else s.split(',').let { LatLng(it[0].toDouble(), it[1].toDouble()) }
    }
)

@Composable
fun CampusMapCard(
    mapView: MapView, // 사용하지 않음 (호환성 유지용)
    modifier: Modifier = Modifier,
    initialCamera: LatLng = LatLng(37.5666102, 126.9783881),
    onExpandRequest: () -> Unit,
    mapViewModel: MapViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // CampusMapCard 전용 MapView 생성 (충돌 방지)
    val campusMapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    DisposableEffect(lifecycle, campusMapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { campusMapView.onStart() }
            override fun onResume(owner: LifecycleOwner) { campusMapView.onResume() }
            override fun onPause(owner: LifecycleOwner) { campusMapView.onPause() }
            override fun onStop(owner: LifecycleOwner) { campusMapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { campusMapView.onDestroy() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    var hasPermission by remember { mutableStateOf(hasLocationPermission()) }
    val askedOnce = rememberSaveable { mutableStateOf(false) }
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
//    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var myLatLng by rememberSaveable(stateSaver = LatLngSaver) { mutableStateOf<LatLng?>(null) }

    var naverMapRef by remember { mutableStateOf<NaverMap?>(null) }
    val movedToMyLocOnce = rememberSaveable { mutableStateOf(false) }
    var markers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var clusterManager by remember { mutableStateOf<MapClusterManager?>(null) }

    @SuppressLint("MissingPermission")
    fun fetchMyLocationOnce() {
        if (!hasLocationPermission()) return
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && myLatLng == null) {
                myLatLng = LatLng(loc.latitude, loc.longitude)

            }
        }
        val cts = CancellationTokenSource()
        val priority = if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        fused.getCurrentLocation(priority, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) myLatLng = LatLng(loc.latitude, loc.longitude)
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (hasPermission) fetchMyLocationOnce()
    }

    LaunchedEffect(Unit) {
        if (!askedOnce.value) {
            askedOnce.value = true
            if (hasLocationPermission()) {
                hasPermission = true
                fetchMyLocationOnce()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    LaunchedEffect(myLatLng, naverMapRef) {
        val map = naverMapRef
        val pos = myLatLng
        if (map != null && pos != null && !movedToMyLocOnce.value) {
            map.moveCamera(CameraUpdate.scrollAndZoomTo(pos, 15.0))
            map.locationOverlay.isVisible = false
            map.locationOverlay.position = pos
            movedToMyLocOnce.value = true
            
            mapViewModel.loadMapContents(
                latitude = pos.latitude,
                longitude = pos.longitude
            )
        }
    }
    
    
    LaunchedEffect(mapViewModel.shouldUpdateClustering, naverMapRef) {
        val map = naverMapRef ?: return@LaunchedEffect
        
        if (mapViewModel.shouldUpdateClustering) {
            clusterManager?.updateMarkers(mapViewModel.mapContents, mapViewModel.mapRecords)
            mapViewModel.clusteringUpdated()
        }
    }

    // 화면 재진입 시 마커 업데이트를 위한 추가 LaunchedEffect
    LaunchedEffect(Unit) {
        // Compose 재구성 시 기존 데이터 확인하여 마커 업데이트
        if (naverMapRef != null && clusterManager != null) {
            if (mapViewModel.mapContents.isNotEmpty() || mapViewModel.mapRecords.isNotEmpty()) {
                android.util.Log.d("CampusMapCard", "🏠 HomeScreen 재구성 - 기존 데이터로 마커 업데이트")
                clusterManager?.updateMarkers(mapViewModel.mapContents, mapViewModel.mapRecords)
            }
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = CardDefaults.shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        ) {
            if (hasPermission && myLatLng == null) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { campusMapView },
                        modifier = Modifier.fillMaxSize(),
                        update = { mv ->
                            // 맵이 이미 초기화되어 있고 데이터가 있으면 마커 업데이트 (뒤로가기 후 재진입 시)
                            if (naverMapRef != null && clusterManager != null) {
                                if (mapViewModel.mapContents.isNotEmpty() || mapViewModel.mapRecords.isNotEmpty()) {
                                    android.util.Log.d("CampusMapCard", "🔄 화면 재진입 - 기존 데이터로 마커 업데이트: contents=${mapViewModel.mapContents.size}, records=${mapViewModel.mapRecords.size}")
                                    clusterManager?.updateMarkers(mapViewModel.mapContents, mapViewModel.mapRecords)
                                }
                            }
                            
                            if (naverMapRef == null) {
                                mv.getMapAsync { map ->
                                    naverMapRef = map
                                    
                                    map.uiSettings.apply {
                                        // 제스처/버튼 모두 끄기 = 화면 고정
                                        isScrollGesturesEnabled = false
                                        isZoomGesturesEnabled = false
                                        isTiltGesturesEnabled = false
                                        isRotateGesturesEnabled = false
                                        isZoomControlEnabled = false
                                        isScaleBarEnabled = false
                                        isCompassEnabled = false
                                        isLocationButtonEnabled = false
                                        isLogoClickEnabled = false
                                        setLogoMargin(-500, -500, 0, 0)
                                    }
                                    
                                    val target = myLatLng ?: initialCamera
                                    map.moveCamera(CameraUpdate.scrollAndZoomTo(target, 15.0))
                                    
                                    // 카메라 이동 후 스타일 적용
                                    var cameraIdleListener: NaverMap.OnCameraIdleListener? = null
                                    cameraIdleListener = NaverMap.OnCameraIdleListener {
                                        try {
                                            map.setCustomStyleId("258120eb-1ebf-4b29-97cf-21df68e09c5c")
                                            android.util.Log.d("CampusMapCard", "카메라 idle 후 커스텀 스타일 적용 성공")
                                            // 리스너 제거 (한 번만 실행)
                                            cameraIdleListener?.let { map.removeOnCameraIdleListener(it) }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CampusMapCard", "카메라 idle 후 커스텀 스타일 적용 실패", e)
                                            e.printStackTrace()
                                        }
                                    }
                                    map.addOnCameraIdleListener(cameraIdleListener)
                                    
                                    // 기본 location overlay 비활성화
                                    map.locationOverlay.isVisible = false
                                    
                                    clusterManager = MapClusterManager(context, map).also {
                                        it.setupClustering()
                                    }
                                    
                                    mapViewModel.loadMapContents(
                                        latitude = target.latitude,
                                        longitude = target.longitude
                                    )
                                }
                            }
                        }
                    )
                    
                    // 모듈화된 위치 마커 관리자 사용
                    LocationMarkerManager(
                        map = naverMapRef,
                        userLocation = myLatLng,
                        hasLocationPermission = hasPermission,
                        enableLottieAnimation = true
                    )
                }

                // 투명 클릭 레이어
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onExpandRequest() }
                )
            }
        }
    }
}
