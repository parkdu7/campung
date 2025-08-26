package com.shinhan.campung.presentation.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.widget.LocationButtonView
import com.naver.maps.map.overlay.Marker
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import com.shinhan.campung.presentation.ui.map.MapClusterManager
import com.shinhan.campung.presentation.ui.map.LocationPermissionManager
import com.shinhan.campung.presentation.ui.map.LocationProvider
import com.shinhan.campung.presentation.ui.map.MapInitializer
import com.shinhan.campung.presentation.ui.map.MapCameraListener
import com.shinhan.campung.presentation.ui.map.ClusterManagerInitializer
import com.shinhan.campung.presentation.ui.components.MapTopHeader
import com.shinhan.campung.presentation.ui.components.HorizontalFilterTags
import com.shinhan.campung.presentation.ui.components.DatePickerDialog
import com.shinhan.campung.data.remote.response.MapContent
import android.util.Log
import com.shinhan.campung.navigation.Route

@Composable
fun FullMapScreen(
    navController: NavController,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    DisposableEffect(lifecycle, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { mapView.onStart() }
            override fun onResume(owner: LifecycleOwner) { mapView.onResume() }
            override fun onPause(owner: LifecycleOwner) { mapView.onPause() }
            override fun onStop(owner: LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val locationPermissionManager = remember { LocationPermissionManager(context) }
    val locationProvider = remember { LocationProvider(context) }
    val mapInitializer = remember { MapInitializer() }
    val clusterManagerInitializer = remember { ClusterManagerInitializer(context, mapViewModel) }

    var hasPermission by remember { mutableStateOf(locationPermissionManager.hasLocationPermission()) }
    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var naverMapRef by remember { mutableStateOf<NaverMap?>(null) }
    var clusterManager by remember { mutableStateOf<MapClusterManager?>(null) }
    var mapCameraListener by remember { mutableStateOf<MapCameraListener?>(null) }
    var highlightedContent by remember { mutableStateOf<MapContent?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun fetchMyLocationOnce() {
        locationProvider.fetchMyLocationOnce(hasPermission) { location ->
            if (location != null && myLatLng == null) {
                myLatLng = location
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationPermissionManager.handlePermissionResult(result) {
            hasPermission = true
            fetchMyLocationOnce()
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermissionManager.hasLocationPermission()) {
            hasPermission = true
            fetchMyLocationOnce()
        } else {
            locationPermissionManager.requestLocationPermission(permissionLauncher)
        }
    }

    // 내 위치 찾히면 카메라 이동
    LaunchedEffect(myLatLng, naverMapRef) {
        val map = naverMapRef
        val pos = myLatLng
        if (map != null && pos != null) {
            map.moveCamera(CameraUpdate.scrollAndZoomTo(pos, 16.0))
            map.locationOverlay.isVisible = true
            map.locationOverlay.position = pos

            mapViewModel.loadMapContents(
                latitude = pos.latitude,
                longitude = pos.longitude
            )
        }
    }

    LaunchedEffect(mapViewModel.shouldUpdateClustering, naverMapRef) {
        val map = naverMapRef ?: return@LaunchedEffect

        if (mapViewModel.shouldUpdateClustering) {
            Log.d("FullMapScreen", "LaunchedEffect에서 클러스터링 업데이트: ${mapViewModel.mapContents.size}개")
            clusterManager?.updateMarkers(mapViewModel.mapContents)
            mapViewModel.clusteringUpdated()
        }
    }

    // 선택된 마커가 변경될 때마다 ClusterManager에 반영
    LaunchedEffect(mapViewModel.selectedMarker) {
        val selectedMarker = mapViewModel.selectedMarker
        Log.d("FullMapScreen", "LaunchedEffect: selectedMarker 변경됨 - ${selectedMarker?.title}")
        if (selectedMarker != null) {
            Log.d("FullMapScreen", "ClusterManager에 마커 선택 요청: ${selectedMarker.title}")
            clusterManager?.selectMarker(selectedMarker)
        } else {
            Log.d("FullMapScreen", "ClusterManager 선택 해제")
            clusterManager?.clearSelection()
        }
    }

    // 툴팁 표시/숨기기 처리 - 더 이상 자동으로 사라지지 않음
    // selectedMarker가 있을 때만 툴팁 표시

    // 하드웨어/소프트웨어 뒤로가기 모두 홈으로
    BackHandler { navController.popBackStack() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(Modifier.fillMaxSize()) {
            // 네이버 지도
            AndroidView(
                factory = { mapView },
                update = { mv ->
                    if (naverMapRef == null) {
                        mv.getMapAsync { map ->
                            naverMapRef = map
                            mapInitializer.setupMapUI(map)

                            clusterManager = clusterManagerInitializer.createClusterManager(map) { centerContent ->
                                highlightedContent = centerContent
                            }

                            mapCameraListener = MapCameraListener(mapViewModel, clusterManager)
                            map.addOnCameraChangeListener(mapCameraListener!!.createCameraChangeListener())
                        }
                    } else {
                        naverMapRef?.let { map ->
                            mapInitializer.setupLocationOverlay(map, hasPermission, myLatLng)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                AndroidView(
                    factory = { ctx -> LocationButtonView(ctx) },
                    update = { btn ->
                        naverMapRef?.let { btn.map = it }
                    }
                )

                // 클릭 오버레이 (FAB 기능 통합)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val pos = myLatLng
                            if (pos != null) {
                                naverMapRef?.moveCamera(CameraUpdate.scrollAndZoomTo(pos, 16.0))
                                naverMapRef?.locationOverlay?.apply {
                                    isVisible = true
                                    position = pos
                                }
                            } else {
                                if (hasPermission) {
                                    fetchMyLocationOnce()
                                } else {
                                    locationPermissionManager.requestLocationPermission(permissionLauncher)
                                }
                            }
                        }
                )
            }

            // 상단 헤더 (오버레이)
            MapTopHeader(
                selectedDate = mapViewModel.selectedDate,
                onBackClick = { navController.popBackStack() },
                onDateClick = {
                    showDatePicker = true
                },
                onFriendClick = {
                    navController.navigate(Route.FRIEND)
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // 필터 태그 (오버레이)
            HorizontalFilterTags(
                selectedTags = mapViewModel.selectedTags,
                onTagClick = { tagId ->
                    mapViewModel.toggleFilterTag(tagId)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            )

        }

        // 날짜 선택 다이얼로그
        if (showDatePicker) {
            DatePickerDialog(
                selectedDate = mapViewModel.selectedDate,
                onDateSelected = { newDate ->
                    mapViewModel.updateSelectedDate(newDate)
                },
                onDismiss = {
                    showDatePicker = false
                }
            )
        }
    }
}