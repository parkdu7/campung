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
import com.shinhan.campung.presentation.ui.components.MapTopHeader
import com.shinhan.campung.presentation.ui.components.HorizontalFilterTags
import com.shinhan.campung.presentation.ui.components.DatePickerDialog
import com.shinhan.campung.presentation.ui.components.GlassTooltipView
import com.shinhan.campung.data.remote.response.MapContent
import android.util.Log
import kotlinx.coroutines.delay

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
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var naverMapRef by remember { mutableStateOf<NaverMap?>(null) }
    var markers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var clusterManager by remember { mutableStateOf<MapClusterManager?>(null) }
    var lastZoomLevel by remember { mutableStateOf(0.0) }
    var lastCameraChangeTime by remember { mutableStateOf(0L) }
    var highlightedContent by remember { mutableStateOf<MapContent?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 툴팁은 이제 InfoWindow로 마커에 직접 붙음

    @SuppressLint("MissingPermission")
    fun fetchMyLocationOnce() {
        if (!hasLocationPermission()) return
        // last + current
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
                            map.uiSettings.apply {
                                isScrollGesturesEnabled = true
                                isZoomGesturesEnabled = true
                                isTiltGesturesEnabled = true
                                isRotateGesturesEnabled = true
                                isZoomControlEnabled = false
                                isScaleBarEnabled = false
                                isCompassEnabled = true
                                isLocationButtonEnabled = false
                            }

                            clusterManager = MapClusterManager(context, map).also { manager ->
                                manager.setupClustering()

                                // 마커 클릭 이벤트 처리 - ViewModel과 연동
                                manager.onMarkerClick = { mapContent ->
                                    Log.d("FullMapScreen", "마커 클릭: ${mapContent.title}")
                                    if (mapViewModel.isMarkerSelected(mapContent)) {
                                        // 이미 선택된 마커 클릭 시 선택 해제
                                        Log.d("FullMapScreen", "이미 선택된 마커 클릭 - 선택 해제")
                                        mapViewModel.clearSelectedMarker()
                                    } else {
                                        // 새 마커 선택
                                        Log.d("FullMapScreen", "새 마커 선택: ${mapContent.title}")
                                        mapViewModel.selectMarker(mapContent)
                                    }
                                    // 여기서 바텀시트나 다이얼로그 등을 호출할 수 있음
                                }

                                // 클러스터 클릭 이벤트 처리
                                manager.onClusterClick = { clusterContents ->
                                    Log.d("FullMapScreen", "클러스터 클릭: ${clusterContents.size}개 아이템")
                                    // 클러스터 클릭 시 선택 해제
                                    mapViewModel.clearSelectedMarker()
                                }

                                // 중앙 마커 변경 이벤트 처리 (기존 로직 유지)
                                manager.onCenterMarkerChanged = { centerContent ->
                                    highlightedContent = centerContent
                                }

                                // 툴팁은 이제 InfoWindow로 처리됨

                                Log.d("FullMapScreen", "ClusterManager 생성됨")
                            }

                            map.addOnCameraChangeListener { _, _ ->
                                val currentTime = System.currentTimeMillis()

                                // 쓰로틀링: 200ms 이내 연속 호출 방지
                                if (currentTime - lastCameraChangeTime < 200) {
                                    return@addOnCameraChangeListener
                                }
                                lastCameraChangeTime = currentTime

                                val center = map.cameraPosition.target
                                val currentZoom = map.cameraPosition.zoom

                                // 툴팁은 InfoWindow로 자동 처리됨

                                // 줌 레벨이 변경된 경우에만 클러스터링 업데이트
                                if (kotlin.math.abs(currentZoom - lastZoomLevel) > 0.5) {
                                    lastZoomLevel = currentZoom
                                    Log.d("FullMapScreen", "줌 레벨 변경: $currentZoom, 마커 개수: ${mapViewModel.mapContents.size}")
                                    clusterManager?.updateMarkers(mapViewModel.mapContents)
                                }

                                // API 요청 (디바운스는 ViewModel에서 처리)
                                mapViewModel.loadMapContents(
                                    latitude = center.latitude,
                                    longitude = center.longitude
                                )

                                // 중앙 마커 찾기
                                Log.d("FullMapScreen", "카메라 변경 - 중앙 마커 찾기 호출")
                                clusterManager?.findCenterMarker()
                            }
                        }
                    } else {
                        naverMapRef?.let { map ->
                            if (myLatLng != null && hasPermission) {
                                map.locationOverlay.isVisible = true
                                map.locationOverlay.position = myLatLng!!
                            } else {
                                map.locationOverlay.isVisible = false
                            }
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
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
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
                    // TODO: 친구 화면 구현
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 필터 태그 (오버레이) - 이게 postType 탭임!
            HorizontalFilterTags(
                selectedTags = mapViewModel.selectedTags,
                onTagClick = { tagId ->
                    mapViewModel.toggleFilterTag(tagId)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            )

            // 툴팁은 이제 InfoWindow로 마커에 직접 붙어서 표시됨
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