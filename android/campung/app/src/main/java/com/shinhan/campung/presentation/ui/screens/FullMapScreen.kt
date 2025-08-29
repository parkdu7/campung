package com.shinhan.campung.presentation.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.shinhan.campung.data.service.LocationSharingManager
import com.shinhan.campung.presentation.ui.map.SharedLocationMarkerManager
import com.shinhan.campung.presentation.ui.map.POIMarkerManager
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
import com.shinhan.campung.presentation.ui.components.WeatherTemperatureDisplay
import com.shinhan.campung.presentation.ui.map.MapClusterManager
import com.shinhan.campung.presentation.ui.map.LocationPermissionManager
import com.shinhan.campung.presentation.ui.map.LocationProvider
import com.shinhan.campung.presentation.ui.map.MapInitializer
import com.shinhan.campung.presentation.ui.map.MapCameraListener
import com.shinhan.campung.presentation.ui.map.ClusterManagerInitializer
import com.shinhan.campung.presentation.ui.components.MapTopHeader
import com.shinhan.campung.presentation.ui.components.HorizontalFilterTags
import com.shinhan.campung.presentation.ui.components.KoreanDatePicker
import com.shinhan.campung.presentation.ui.components.POIFilterTags
import com.shinhan.campung.presentation.ui.components.POIDetailDialog
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.model.MapRecord
import com.shinhan.campung.presentation.ui.components.AudioPlayer
import android.util.Log
import com.shinhan.campung.navigation.Route
import com.shinhan.campung.presentation.ui.components.MapBottomSheetContent
import com.shinhan.campung.presentation.ui.components.MixedMapBottomSheetContent
import com.shinhan.campung.presentation.ui.components.AnimatedMapTooltip
import com.shinhan.campung.presentation.ui.components.MyLocationMarker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.shinhan.campung.R
import com.shinhan.campung.presentation.ui.components.RecordUploadDialog

// 새로운 바텀시트 컴포넌트 imports
import com.shinhan.campung.presentation.ui.components.bottomsheet.*
import com.shinhan.campung.presentation.viewmodel.RecordUploadViewModel

@Composable
fun FullMapScreen(
    navController: NavController,
    mapView: MapView, // 외부에서 주입받음
    mapViewModel: MapViewModel = hiltViewModel()
) {
    // --- 녹음 다이얼로그 on/off
    var showRecordDialog by remember { mutableStateOf(false) }

    // --- 업로드용 VM
    val recordUploadVm: RecordUploadViewModel = hiltViewModel()
    val recordUi by recordUploadVm.ui.collectAsState()

    // --- 오디오 권한 런처
    var shouldStartAfterPermission by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && shouldStartAfterPermission) {
            // 다이얼로그 안에서 startRecording()을 다시 트리거할 수 있도록
            // 플래그만 true로 두고 다이얼로그의 버튼 클릭 로직에서 처리되게 함
        }
        shouldStartAfterPermission = false
    }

    // LocationSharingManager는 MapViewModel에서 이미 주입받았으므로 거기서 가져옴
    val locationSharingManager = mapViewModel.locationSharingManager
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel states
    val bottomSheetContents by mapViewModel.bottomSheetContents.collectAsState()
    val bottomSheetItems by mapViewModel.bottomSheetItems.collectAsState()
    val isBottomSheetExpanded by mapViewModel.isBottomSheetExpanded.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val tooltipState by mapViewModel.tooltipState.collectAsState()
    val serverWeather by mapViewModel.serverWeather.collectAsState()
    val serverTemperature by mapViewModel.serverTemperature.collectAsState()

    val calculated = remember(mapViewModel.mapContents) {
        calculateWeatherInfo(mapViewModel.mapContents)
    }
    // ✅ 서버값이 있으면 우선 사용, 없으면 계산된 값 사용
    val uiWeather = normalizeWeather(serverWeather) ?: normalizeWeather(calculated.weather)
    val uiTemperature = serverTemperature ?: calculated.temperature

    Log.d("FullMapScreen", "🎯 최종 UI 데이터 - serverWeather: '$serverWeather'(${serverTemperature}°) → uiWeather: '$uiWeather'($uiTemperature°)")


    val sharedLocations by locationSharingManager.sharedLocations.collectAsState()

    // POI 관련 상태
    val poiData by mapViewModel.poiData.collectAsState()
    val isPOIVisible by mapViewModel.isPOIVisible.collectAsState()
    val selectedPOICategory by mapViewModel.selectedPOICategory.collectAsState()
    val isPOILoading by mapViewModel.isPOILoading.collectAsState()
    val selectedPOI by mapViewModel.selectedPOI.collectAsState()
    val showPOIDialog by mapViewModel.showPOIDialog.collectAsState()
    val isLoadingPOIDetail by mapViewModel.isLoadingPOIDetail.collectAsState()
    val currentPlayingRecord by mapViewModel.currentPlayingRecord.collectAsState()
    val currentUserId by mapViewModel.currentUserId.collectAsState()

    // 위치 공유 브로드캐스트 수신
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                android.util.Log.d("FullMapScreen", "브로드캐스트 수신됨 - action: ${intent?.action}")

                if (intent?.action == "com.shinhan.campung.LOCATION_SHARED") {
                    android.util.Log.d("FullMapScreen", "위치 공유 브로드캐스트 처리 시작")

                    val userName = intent.getStringExtra("userName")
                    val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull()
                    val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull()
                    val displayUntil = intent.getStringExtra("displayUntil")
                    val shareId = intent.getStringExtra("shareId")

                    android.util.Log.d("FullMapScreen", "브로드캐스트 데이터: userName=$userName, lat=$latitude, lng=$longitude, displayUntil=$displayUntil, shareId=$shareId")

                    if (userName == null || latitude == null || longitude == null || displayUntil == null || shareId == null) {
                        android.util.Log.e("FullMapScreen", "브로드캐스트 데이터 누락 - 처리 중단")
                        return
                    }

                    android.util.Log.d("FullMapScreen", "LocationSharingManager.addSharedLocation 호출")
                    locationSharingManager.addSharedLocation(
                        userName, latitude, longitude, displayUntil, shareId
                    )
                } else {
                    android.util.Log.d("FullMapScreen", "다른 액션의 브로드캐스트 무시")
                }
            }
        }

        val intentFilter = IntentFilter("com.shinhan.campung.LOCATION_SHARED")
        android.util.Log.d("FullMapScreen", "브로드캐스트 수신기 등록 중 - action: com.shinhan.campung.LOCATION_SHARED")

        // 전역 브로드캐스트 수신기 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            android.util.Log.d("FullMapScreen", "전역 브로드캐스트 수신기 등록 완료 (API 33+)")
        } else {
            context.registerReceiver(receiver, intentFilter)
            android.util.Log.d("FullMapScreen", "전역 브로드캐스트 수신기 등록 완료 (API <33)")
        }

        // LocalBroadcastManager도 등록 (더 안전함)
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(receiver, intentFilter)
            android.util.Log.d("FullMapScreen", "LocalBroadcast 수신기도 등록 완료")
        } catch (e: Exception) {
            android.util.Log.e("FullMapScreen", "LocalBroadcast 수신기 등록 실패", e)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
                android.util.Log.d("FullMapScreen", "전역 브로드캐스트 수신기 해제 완료")
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("FullMapScreen", "전역 브로드캐스트 수신기 해제 실패 (이미 해제됨)")
            }

            try {
                androidx.localbroadcastmanager.content.LocalBroadcastManager
                    .getInstance(context)
                    .unregisterReceiver(receiver)
                android.util.Log.d("FullMapScreen", "LocalBroadcast 수신기 해제 완료")
            } catch (e: Exception) {
                android.util.Log.w("FullMapScreen", "LocalBroadcast 수신기 해제 실패", e)
            }
        }
    }

    // 화면 크기
    val screenHeight = configuration.screenHeightDp.dp
    val itemHeight = 120.dp
    val padding = 16.dp
    val itemSpacing = 8.dp
    val dragHandleHeight = 30.dp

    // 새로운 바텀시트 상태
    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.PartiallyExpanded,
        confirmValueChange = { targetValue ->
            // 빈 상태에서는 확장을 허용하지 않음 (단, 로딩 중에는 허용)
            if (bottomSheetContents.isEmpty() && !isLoading) {
                targetValue == BottomSheetValue.PartiallyExpanded
            } else {
                true
            }
        }
    )

    // 화면 높이 계산
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density)
    val statusBarHeight = WindowInsets.statusBars.getTop(density)
    val availableHeight = screenHeight - with(density) {
        (navigationBarHeight + statusBarHeight).toDp()
    }

    // 동적 컨텐츠 높이 계산 (통합 바텀시트 지원)
    val dynamicContentHeight = remember(bottomSheetContents.size, bottomSheetItems.size, isLoading) {
        // 통합 바텀시트가 있으면 우선 사용, 없으면 기존 방식
        val itemCount = if (bottomSheetItems.isNotEmpty()) bottomSheetItems.size else bottomSheetContents.size

        when {
            isLoading -> dragHandleHeight + padding * 2 + itemHeight
            itemCount == 0 -> dragHandleHeight
            itemCount == 1 -> dragHandleHeight + itemHeight + padding * 2
            itemCount == 2 -> dragHandleHeight + (itemHeight * 2) + itemSpacing + padding * 2
            else -> {
                val maxHeight = screenHeight * 0.5f
                val calculatedHeight = dragHandleHeight + padding * 2 + (itemHeight * itemCount) + (itemSpacing * (itemCount - 1))
                minOf(maxHeight, calculatedHeight)
            }
        }
    }

    // LocationButton 오프셋 계산 (바텀시트 상태에서 가져옴)
    val locationButtonOffsetY = with(density) {
        val positions = bottomSheetState.positions
        if (positions != null) {
            val currentOffset = (bottomSheetState.offsetY - positions.partiallyExpanded).coerceAtMost(0f)
            currentOffset.toDp()
        } else {
            0.dp
        }
    }

    // 지도 설정 및 마커 생명주기 관리
    DisposableEffect(lifecycle, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
                Log.d("FullMapScreen", "🔄 onStart - 화면 복귀")
            }
            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
                Log.d("FullMapScreen", "▶️ onResume - 화면 활성화")
            }
            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
                Log.d("FullMapScreen", "⏸️ onPause - 화면 비활성화, 마커 정리는 나중에 처리됨")
            }
            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
                Log.d("FullMapScreen", "⏹️ onStop - 화면 중지")
            }
            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
                Log.d("FullMapScreen", "💀 onDestroy - 화면 파괴")
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            Log.d("FullMapScreen", "🧹 DisposableEffect 정리")
        }
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
    var mapViewportManager by remember { mutableStateOf<com.shinhan.campung.presentation.ui.map.MapViewportManager?>(null) }
    var mapInteractionController by remember { mutableStateOf<com.shinhan.campung.presentation.ui.map.MapInteractionController?>(null) }
    var highlightedContent by remember { mutableStateOf<MapContent?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun fetchMyLocationOnce() {
        locationProvider.fetchMyLocationOnce(hasPermission) { location ->
            if (location != null && myLatLng == null) {
                myLatLng = location
            }
        }
    }

    // 위치 공유 마커 매니저 (모듈화됨)
    val sharedLocationMarkerManager = remember { SharedLocationMarkerManager() }

    // POI 마커 매니저 (모듈화됨)
    var poiMarkerManager by remember { mutableStateOf<POIMarkerManager?>(null) }

    // 마커 매니저들의 생명주기 관리 (앱 종료 시에만)
    DisposableEffect(Unit) { // 한 번만 실행되도록 Unit 의존성 사용
        Log.d("FullMapScreen", "🎯 마커 매니저 생명주기 관리 시작")

        onDispose {
            Log.d("FullMapScreen", "🧹 화면 완전 종료 시 마커 매니저 정리 시작")
            // cleanup()은 완전한 앱/화면 종료 시에만 호출 (콜백도 정리됨)
            clusterManager?.cleanup()
            poiMarkerManager?.clearPOIMarkers()
            sharedLocationMarkerManager.clearAllMarkers()
            Log.d("FullMapScreen", "✅ 모든 마커 매니저 완전 정리 완료")
        }
    }

    // 위치 공유 데이터 변경 시 마커 업데이트
    LaunchedEffect(sharedLocations) {
        naverMapRef?.let { map ->
            sharedLocationMarkerManager.updateSharedLocationMarkers(map, sharedLocations)
        }
    }

    // POI 데이터 변경 시 마커 업데이트 (중복 호출 방지)
    LaunchedEffect(poiData, isPOIVisible) {
        Log.d("FullMapScreen", "🏪 POI LaunchedEffect 트리거 - isPOIVisible: $isPOIVisible, poiData: ${poiData.size}개")

        naverMapRef?.let { map ->
            poiMarkerManager?.let { manager ->
                if (isPOIVisible && poiData.isNotEmpty()) {
                    Log.d("FullMapScreen", "🏪 POI 마커 표시 시작")
                    manager.showPOIMarkers(poiData)
                } else {
                    Log.d("FullMapScreen", "🏪 POI 마커 클리어")
                    manager.clearPOIMarkers()
                }
            } ?: Log.w("FullMapScreen", "🏪 POI 마커 매니저가 null")
        } ?: Log.w("FullMapScreen", "🏪 NaverMap이 null")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationPermissionManager.handlePermissionResult(result) {
            hasPermission = true
            fetchMyLocationOnce()
        }
    }

    val refreshIdFlow = remember(navController) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow<Long?>("map_refresh_content_id", null)
    }
    val refreshId by (refreshIdFlow?.collectAsState() ?: remember { mutableStateOf<Long?>(null) })

    LaunchedEffect(refreshId, naverMapRef) {
        val id = refreshId ?: return@LaunchedEffect
        Log.d("FullMapScreen", "🎯 글 작성 후 리프레시 처리 시작 - ID: $id")

        // NaverMap이 준비될 때까지 기다림
        if (naverMapRef == null) {
            Log.w("FullMapScreen", "⚠️ NaverMap이 아직 준비되지 않음 - 리프레시 지연")
            return@LaunchedEffect
        }

        // 현재 화면 중심/반경으로 강제 리로드
        val center = naverMapRef?.cameraPosition?.target
        val lat = center?.latitude ?: mapViewModel.getLastKnownLocation()?.first ?: 0.0
        val lng = center?.longitude ?: mapViewModel.getLastKnownLocation()?.second ?: 0.0
        val radius = naverMapRef?.let {
            com.shinhan.campung.presentation.ui.map.MapBoundsCalculator.calculateVisibleRadius(it)
        } ?: 2000

        Log.d("FullMapScreen", "📍 리프레시 위치: ($lat, $lng), 반경: ${radius}m")

        // 하이라이트 예약과 강제 리로드
        mapViewModel.requestHighlight(id)
        Log.d("FullMapScreen", "🔍 하이라이트 예약 완료 - ID: $id")

        // 서버 동기화 대기 후 한 번만 리로드
        kotlinx.coroutines.delay(1000)
        mapViewModel.loadMapContents(lat, lng, radius = radius, force = true)

        // 원샷 처리
        navController.currentBackStackEntry?.savedStateHandle?.set("map_refresh_content_id", null)
        Log.d("FullMapScreen", "✅ 리프레시 ID 초기화 완료")
    }

    // 업로드 성공 콜백 설정
    LaunchedEffect(Unit) {
        recordUploadVm.setOnUploadSuccessCallback { latitude, longitude ->
            Log.d("FullMapScreen", "🎵 녹음 업로드 성공 - 맵 새로고침: ($latitude, $longitude)")
            mapViewModel.loadMapContents(latitude, longitude, force = true)
        }
    }

    LaunchedEffect(recordUi.successMessage, recordUi.errorMessage) {
        recordUi.successMessage?.let {
            // 업로드 성공 → 다이얼로그 닫고 메시지 소비
            showRecordDialog = false
            recordUploadVm.consumeMessages()
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
        recordUi.errorMessage?.let {
            recordUploadVm.consumeMessages()
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
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

    // AudioPlayer가 표시될 때 지도 상호작용 제어 및 바텀시트 내리기
    LaunchedEffect(currentPlayingRecord, naverMapRef) {
        naverMapRef?.let { map ->
            map.uiSettings.apply {
                isScrollGesturesEnabled = currentPlayingRecord == null
                isZoomGesturesEnabled = currentPlayingRecord == null
                isTiltGesturesEnabled = currentPlayingRecord == null
                isRotateGesturesEnabled = currentPlayingRecord == null
            }
        }
        
        // AudioPlayer가 표시될 때 바텀시트 내리기
        if (currentPlayingRecord != null) {
            bottomSheetState.animateTo(BottomSheetValue.PartiallyExpanded)
            mapViewModel.updateBottomSheetExpanded(false)
        }
    }

    // 내 위치 찾히면 카메라 이동
    LaunchedEffect(myLatLng, naverMapRef) {
        val map = naverMapRef
        val pos = myLatLng
        if (map != null && pos != null) {
            map.moveCamera(CameraUpdate.scrollAndZoomTo(pos, 16.0))
            map.locationOverlay.isVisible = false
            map.locationOverlay.position = pos

            // 초기 로드 - 핫 콘텐츠를 먼저 로드
            Log.d("FullMapScreen", "🔥 초기 진입 - 핫 콘텐츠 로드")
            mapViewModel.loadHotContents()
            
            // 마커 데이터도 로드 (백그라운드)
            naverMapRef?.let { map ->
                val radius = com.shinhan.campung.presentation.ui.map.MapBoundsCalculator.calculateVisibleRadius(map)
                Log.d("FullMapScreen", "🎯 초기 위치 기반 마커 로드: (${pos.latitude}, ${pos.longitude}), 반경: ${radius}m")
                mapViewModel.loadMapContents(
                    latitude = pos.latitude,
                    longitude = pos.longitude,
                    radius = radius,
                    force = true  // 초기 로드는 항상 강제 실행
                )
            } ?: run {
                // NaverMap이 아직 준비되지 않았으면 기본 방식으로 강제 로드
                Log.d("FullMapScreen", "🎯 NaverMap 준비 전 기본 마커 로드: (${pos.latitude}, ${pos.longitude})")
                mapViewModel.loadMapContents(
                    latitude = pos.latitude,
                    longitude = pos.longitude,
                    force = true  // 초기 로드는 항상 강제 실행
                )
            }
        }
    }

    // 카메라 리스너들을 개별적으로 관리하되 애니메이션 쌓임 방지
    DisposableEffect(naverMapRef, mapCameraListener, mapViewportManager) {
        val map = naverMapRef ?: return@DisposableEffect onDispose { }
        
        // 각 리스너의 인스턴스를 저장
        val cameraListener = mapCameraListener?.createCameraChangeListener()
        val viewportListener = mapViewportManager?.createCameraChangeListener()
        
        // 툴팁 업데이트용 별도 리스너 - 쓰로틀링 강화
        var lastTooltipUpdateTime = 0L
        var isZoomInProgress = false
        var zoomEndTimer: kotlinx.coroutines.Job? = null
        
        val tooltipListener = NaverMap.OnCameraChangeListener { reason, animated ->
            val currentTime = System.currentTimeMillis()
            
            // 줌 중인지 판단 (animated=true이고 빠른 연속 호출)
            if (animated && (currentTime - lastTooltipUpdateTime < 100)) {
                isZoomInProgress = true
                
                // 줌 종료 타이머 설정
                zoomEndTimer?.cancel()
                zoomEndTimer = kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(200) // 200ms 후 줌 종료로 판단
                    isZoomInProgress = false
                }
            }
            
            // 줌 중이면 툴팁 업데이트 스킵
            if (isZoomInProgress) {
                return@OnCameraChangeListener
            }
            
            // 쓰로틀링 강화 (100ms)
            if (currentTime - lastTooltipUpdateTime < 100) {
                return@OnCameraChangeListener
            }
            lastTooltipUpdateTime = currentTime
            
            // 툴팁 위치 업데이트
            if (tooltipState.isVisible && tooltipState.content != null) {
                val content = tooltipState.content!!
                val latLng = com.naver.maps.geometry.LatLng(content.location.latitude, content.location.longitude)
                val screenPoint = map.projection.toScreenLocation(latLng)
                val newPosition = androidx.compose.ui.geometry.Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                mapViewModel.updateTooltipPosition(newPosition)
            }
        }
        
        // 각 리스너 등록
        cameraListener?.let { map.addOnCameraChangeListener(it) }
        viewportListener?.let { map.addOnCameraChangeListener(it) }
        map.addOnCameraChangeListener(tooltipListener)
        
        onDispose {
            // 정확한 인스턴스로 리스너 제거
            cameraListener?.let { map.removeOnCameraChangeListener(it) }
            viewportListener?.let { map.removeOnCameraChangeListener(it) }
            map.removeOnCameraChangeListener(tooltipListener)
        }
    }

    // 클러스터링 업데이트 - shouldUpdateClustering만 의존성으로 사용 (중복 실행 방지)
    LaunchedEffect(mapViewModel.shouldUpdateClustering, naverMapRef) {
        val map = naverMapRef ?: return@LaunchedEffect
        
        // shouldUpdateClustering이 true일 때만 실행
        if (!mapViewModel.shouldUpdateClustering) {
            return@LaunchedEffect
        }

        android.util.Log.d("FullMapScreen", "📊 클러스터링 LaunchedEffect 시작 - Contents: ${mapViewModel.mapContents.size}, Records: ${mapViewModel.mapRecords.size}")

        if (mapViewModel.mapContents.isNotEmpty() || mapViewModel.mapRecords.isNotEmpty()) {
            android.util.Log.d("FullMapScreen", "🔄 클러스터링 업데이트 시작")
            try {
                clusterManager?.updateMarkers(mapViewModel.mapContents, mapViewModel.mapRecords) {
                    android.util.Log.d("FullMapScreen", "✅ 클러스터링 업데이트 완료")
                    mapViewModel.onClusteringCompleted()
                }
            } catch (e: Exception) {
                android.util.Log.e("FullMapScreen", "❌ 클러스터링 업데이트 실패", e)
                mapViewModel.onClusteringCompleted()
            }
        } else {
            android.util.Log.d("FullMapScreen", "🧹 빈 데이터로 클러스터링 클리어")
            clusterManager?.clearMarkers()
            mapViewModel.onClusteringCompleted()
        }
    }

    // 선택된 마커가 변경될 때마다 ClusterManager에 반영
    LaunchedEffect(mapViewModel.selectedMarker) {
        val selectedMarker = mapViewModel.selectedMarker
        if (selectedMarker != null) {
            clusterManager?.selectMarker(selectedMarker)
        } else if (mapViewModel.selectedRecord == null) {
            clusterManager?.clearSelection()
        }
    }

    // 선택된 Record가 변경될 때마다 ClusterManager에 반영
    LaunchedEffect(mapViewModel.selectedRecord) {
        val selectedRecord = mapViewModel.selectedRecord
        if (selectedRecord != null) {
            clusterManager?.selectRecordMarker(selectedRecord)
        } else if (mapViewModel.selectedMarker == null) {
            clusterManager?.clearSelection()
        }
    }

    // 툴팁 표시/숨기기 처리 - 더 이상 자동으로 사라지지 않음
    // selectedMarker가 있을 때만 툴팁 표시

    // 뒤로가기 버튼 처리
    BackHandler {
        when {
            mapViewModel.selectedRecord != null -> {
                // Record가 선택되어 있으면 Record 선택 해제 (오디오 플레이어 닫기)
                mapViewModel.stopRecord()
                clusterManager?.clearSelection()
            }
            mapViewModel.selectedMarker != null || clusterManager?.selectedClusterMarker != null -> {
                // 마커나 클러스터가 선택되어 있으면 선택 해제
                mapViewModel.clearSelectedMarker()
                clusterManager?.clearSelection()
            }
            else -> {
                // 화면 나가기 전 모든 마커 정리
                Log.d("FullMapScreen", "🔙 뒤로가기 - 모든 마커 정리 시작")
                clusterManager?.clearMarkers()
                poiMarkerManager?.clearPOIMarkers()
                sharedLocationMarkerManager.clearAllMarkers()
                Log.d("FullMapScreen", "✅ 뒤로가기 - 마커 정리 완료")
                navController.popBackStack()
            }
        }
    }


    // 바텀시트 상태 변화 추적
    LaunchedEffect(isBottomSheetExpanded) {
        android.util.Log.d("FullMapScreen", "🎯 [STATE] isBottomSheetExpanded 변화: $isBottomSheetExpanded")
        try {
            if (isBottomSheetExpanded) {
                bottomSheetState.animateTo(BottomSheetValue.Expanded)
                android.util.Log.d("FullMapScreen", "✅ [STATE] 바텀시트 확장 호출됨")
            } else {
                bottomSheetState.animateTo(BottomSheetValue.Hidden)
                android.util.Log.d("FullMapScreen", "❌ [STATE] 바텀시트 숨김 호출됨")
            }
        } catch (e: Exception) {
            android.util.Log.e("FullMapScreen", "❌ [STATE] 바텀시트 상태 변경 실패", e)
        }
    }

    // 바텀시트 내용 변화 추적 (통합 바텀시트 포함)
    LaunchedEffect(bottomSheetContents.size, bottomSheetItems.size) {
        android.util.Log.d("FullMapScreen", "🎯 [STATE] bottomSheetContents.size 변화: ${bottomSheetContents.size}, bottomSheetItems.size: ${bottomSheetItems.size}")
    }

    // 로딩 상태 변화 추적
    LaunchedEffect(isLoading) {
        android.util.Log.d("FullMapScreen", "🎯 [STATE] isLoading 변화: $isLoading")
    }

    // 바텀시트 상태 실시간 추적 - 사용자가 직접 드래그했을 때도 ViewModel에 반영
    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }
            .collect { currentValue ->
                val isExpanded = currentValue == BottomSheetValue.Expanded
                if (isBottomSheetExpanded != isExpanded) {
                    mapViewModel.updateBottomSheetExpanded(isExpanded)
                }
            }
    }


    // 마커 클릭시 자동 확장 - SideEffect로 즉시 반응 (단, 실제 마커 선택이 있을 때만)
    if (isLoading && mapViewModel.selectedMarkerId.collectAsState().value != null) {
        SideEffect {
            coroutineScope.launch {
                bottomSheetState.snapTo(BottomSheetValue.Expanded)
            }
        }
    }

    // 바텀시트 콘텐츠 변화에 따른 상태 조절 (통합 바텀시트 지원)
    LaunchedEffect(bottomSheetContents.size, bottomSheetItems.size, isLoading) {
        if (!isLoading) {
            val totalItems = if (bottomSheetItems.isNotEmpty()) bottomSheetItems.size else bottomSheetContents.size
            when {
                totalItems == 0 -> {
                    bottomSheetState.snapTo(BottomSheetValue.PartiallyExpanded)
                }
                totalItems >= 1 -> {
                    bottomSheetState.snapTo(BottomSheetValue.Expanded)
                }
            }
        }
    }

    // 바텀시트 확장/축소 상태 동기화 (통합 바텀시트 지원)
    LaunchedEffect(isBottomSheetExpanded) {
        val hasItems = bottomSheetItems.isNotEmpty() || bottomSheetContents.isNotEmpty()
        if (isBottomSheetExpanded && (hasItems || isLoading)) {
            bottomSheetState.snapTo(BottomSheetValue.Expanded)
        } else if (!isBottomSheetExpanded && hasItems && !isLoading) {
            bottomSheetState.animateTo(BottomSheetValue.PartiallyExpanded)
        }
    }


    var isCenterOnMyLocation by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 전체 화면 지도 - Surface 블록을 제거하고 직접 Box로 변경
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 네이버 지도
                AndroidView(
                    factory = { mapView },
                    update = { mv ->
                        if (naverMapRef == null) {
                            mv.getMapAsync { map ->
                                naverMapRef = map
                                mapInitializer.setupMapUI(map)

                                android.util.Log.d("FullMapScreen", "🚀 [INIT] ClusterManager 생성 시작")
                                clusterManager =
                                    clusterManagerInitializer.createClusterManager(map) { centerContent ->
                                        highlightedContent = centerContent
                                    }
                                android.util.Log.d("FullMapScreen", "✅ [INIT] ClusterManager 생성 완료")
                                android.util.Log.d("FullMapScreen", "🔗 [INIT] clusterManager.onMarkerClick: ${clusterManager?.onMarkerClick}")

                                // POI 마커 매니저 초기화
                                poiMarkerManager = POIMarkerManager(context, map, coroutineScope).apply {
                                    onPOIClick = { poi ->
                                        android.util.Log.d("FullMapScreen", "🏪 POI 마커 클릭됨: ${poi.name}")
                                        mapViewModel.onPOIClick(poi)
                                    }
                                }
                                android.util.Log.d("FullMapScreen", "🏪 POI 마커 매니저 초기화 완료")

                                // 👇 카메라가 '움직이는 동안' 계속 호출됨: 아이콘 실시간 갱신
                                map.addOnCameraChangeListener { _, _ ->
                                    val me = myLatLng
                                    if (me == null) {
                                        if (isCenterOnMyLocation) isCenterOnMyLocation = false
                                    } else {
                                        val center = map.cameraPosition.target
                                        val dist = distanceMeters(center, me)
                                        val threshold = 30f
                                        val nowCentered = dist <= threshold
                                        if (isCenterOnMyLocation != nowCentered) {
                                            isCenterOnMyLocation = nowCentered   // 🔁 즉시 토글 (btn_mylocation ↔ btn_location)
                                        }
                                    }
                                }


                                // 지도 상호작용 컨트롤러 생성
                            val interactionController = com.shinhan.campung.presentation.ui.map.MapInteractionController(mapViewModel).apply {
                                setNaverMap(map)
                            }

                            // 카메라 리스너와 뷰포트 관리자 초기화 (실제 리스너 등록은 LaunchedEffect에서 통합 처리)
                                mapCameraListener = MapCameraListener(mapViewModel, clusterManager, interactionController)

                            // 뷰포트 관리자 초기화
                            mapViewportManager = com.shinhan.campung.presentation.ui.map.MapViewportManager(mapViewModel, coroutineScope).apply {
                                setNaverMap(map) // NaverMap 참조 설정
                            }

                                // 지도 클릭 시 마커 및 클러스터 선택 해제
                                map.setOnMapClickListener { _, _ ->
                                    when {
                                        mapViewModel.selectedRecord != null -> {
                                            // Record 선택 해제 (오디오 플레이어 닫기)
                                            mapViewModel.stopRecord()
                                            clusterManager?.clearSelection()
                                        }
                                        mapViewModel.selectedMarker != null || clusterManager?.selectedClusterMarker != null -> {
                                            // Content 마커나 클러스터 선택 해제
                                            mapViewModel.clearSelectedMarker()
                                            clusterManager?.clearSelection()
                                        }
                                    }
                                }

                            }
                        } else {
                            naverMapRef?.let { map ->
                                mapInitializer.setupLocationOverlay(map, mapView, hasPermission, myLatLng)

                                // 위치 공유 마커 업데이트 (모듈화된 매니저 사용)
                                sharedLocationMarkerManager.updateSharedLocationMarkers(map, sharedLocations)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 내 위치 Lottie 애니메이션 마커
                myLatLng?.let { currentLocation ->
                    naverMapRef?.let { map ->
                        MyLocationMarker(
                            map = map,
                            location = currentLocation,
                            modifier = Modifier.zIndex(1f) // 지도 위에, UI 요소들 아래에
                        )
                    }
                }

                // LocationButton - 바텀시트와 함께 움직임 (커스텀 아이콘 버전)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            bottom = 16.dp + dragHandleHeight
                        )
                        .offset(y = locationButtonOffsetY)
                        .size(40.dp)     // 터치 영역 고정
                        .zIndex(3f)      // 바텀시트/지도 위에 노출되도록
                ) {
                    val locationIcon = if (isCenterOnMyLocation) R.drawable.btn_mylocation else R.drawable.btn_location

                    Image(
                        painter = painterResource(locationIcon),
                        contentDescription = if (isCenterOnMyLocation) "내 위치 모드" else "내 위치",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val pos = myLatLng
                                if (pos != null) {
                                    // 내 위치로 카메라 이동 + 오버레이 표시
                                    naverMapRef?.moveCamera(CameraUpdate.scrollAndZoomTo(pos, 16.0))
                                    naverMapRef?.locationOverlay?.apply {
                                        isVisible = false
                                        position = pos
                                    }
                                    isCenterOnMyLocation = true   // 아이콘: btn_mylocation
                                } else {
                                    // 위치 없으면 한 번 더 시도/권한 요청
                                    if (hasPermission) fetchMyLocationOnce()
                                    else locationPermissionManager.requestLocationPermission(permissionLauncher)
                                }
                            }
                    )
                }

                // 플로팅 버튼 상태 관리
                var isFabExpanded by remember { mutableStateOf(false) }

                // ✅ 메인 FAB 아이콘 리소스 상태 (초기: btn_add2)
                var mainFabIconRes by remember { mutableStateOf(R.drawable.btn_add2) }

                // 회전 애니메이션 (열릴 때 45°, 닫힐 때 0°)
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isFabExpanded) 45f else 0f,
                    animationSpec = tween(300),
                    label = "fab_rotation"
                )

                // ✅ 아이콘 전환 타이밍 제어 (복귀 없음)
                LaunchedEffect(isFabExpanded) {
                    if (isFabExpanded) {
                        // 열 때: btn_add로 바꾸고 45° 회전 (animateFloatAsState가 회전)
                        mainFabIconRes = R.drawable.btn_add
                    } else {
                        // 닫을 때: btn_add3로 바꾸고 0°까지 회전
                        mainFabIconRes = R.drawable.btn_add2
                        // 더 이상 btn_add2로 복귀하지 않음
                    }
                }



                // 확장 가능한 플로팅 액션 버튼 - 우측 하단
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = 8.dp + dragHandleHeight // 바텀시트 드래그 핸들 높이만큼 위로
                        )
                        .offset(y = locationButtonOffsetY) // 바텀시트와 함께 움직임
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 확장된 상태에서 보이는 버튼들 (아래에서 위로 나타남)
                        AnimatedVisibility(
                            visible = isFabExpanded,
                            enter = slideInVertically(
                                initialOffsetY = { it }, // 양수 = 아래에서 위로
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(
                                targetOffsetY = { it }, // 양수 = 위에서 아래로 사라짐
                                animationSpec = tween(200)
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 펜 버튼
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            // 메뉴 닫기
                                            isFabExpanded = false
                                            // 글쓰기 화면으로 이동
                                            navController.navigate(Route.WRITE_POST)
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.btn_post),
                                        contentDescription = "글쓰기",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // 녹음등록 버튼
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            // 다이얼로그 열기
                                            showRecordDialog = true
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.btn_record),
                                        contentDescription = "녹음 이동",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // 게시판 버튼
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            // TODO: 게시판 기능 구현
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.btn_board),
                                        contentDescription = "게시판 이동",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp) // 메인 버튼은 조금 더 크게
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    // 토글 기능
                                    isFabExpanded = !isFabExpanded
                                }
                        ) {
                            Image(
                                painter = painterResource(mainFabIconRes),   // ✅ 여기!
                                contentDescription = if (isFabExpanded) "메뉴 닫기" else "메뉴 열기",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotationAngle) // 45도 회전 애니메이션
                            )
                        }
                    }
                }

                // 바텀시트 컴포넌트
                MapDraggableBottomSheet(
                    state = bottomSheetState,
                    screenHeight = screenHeight,
                    availableHeight = availableHeight,
                    contentHeight = dynamicContentHeight,
                    dragHandleHeight = dragHandleHeight
                ) {
                    // 통합 바텀시트 사용 (기존 방식 하위 호환성 유지)
                    if (bottomSheetItems.isNotEmpty()) {
                        // 새로운 통합 방식 (MapItem 리스트 사용)
                        MixedMapBottomSheetContent(
                            items = bottomSheetItems,
                            isLoading = isLoading,
                            isInteractionEnabled = bottomSheetItems.isNotEmpty() || isLoading,
                            navigationBarHeight = with(density) { navigationBarHeight.toDp() },
                            statusBarHeight = with(density) { statusBarHeight.toDp() },
                            currentPlayingRecord = currentPlayingRecord,
                            isPlaying = false, // TODO: 오디오 플레이어 상태와 연결
                            onContentClick = { content ->
                                navController.navigate("${Route.CONTENT_DETAIL}/${content.contentId}")
                            },
                            onRecordClick = { record ->
                                // Record 클릭 시 추가 동작 (필요시)
                                Log.d("FullMapScreen", "Record 클릭: ${record.recordUrl}")
                            },
                            onRecordPlayClick = { record ->
                                mapViewModel.playRecord(record)
                            }
                        )
                    } else {
                        // 기존 방식 (하위 호환성)
                        MapBottomSheetContent(
                            contents = bottomSheetContents,
                            isLoading = isLoading,
                            isInteractionEnabled = bottomSheetContents.isNotEmpty() || isLoading,
                            navigationBarHeight = with(density) { navigationBarHeight.toDp() },
                            statusBarHeight = with(density) { statusBarHeight.toDp() },
                            onContentClick = { content ->
                                navController.navigate("${Route.CONTENT_DETAIL}/${content.contentId}")
                            }
                        )
                    }
                }


                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth()
                        .zIndex(2f)
                ) {
                    MapTopHeader(
                        selectedDate = mapViewModel.selectedDate,
                        onBackClick = { navController.popBackStack() },
                        onDateClick = { showDatePicker = true },
                        onPreviousDate = { mapViewModel.selectPreviousDate() },
                        onNextDate = { mapViewModel.selectNextDate() },
                        onFriendClick = { navController.navigate(Route.FRIEND) }
                    )
                }


                // 필터 태그 (오버레이)
                HorizontalFilterTags(
                    selectedTags = mapViewModel.selectedTags,
                    onTagClick = { tagId -> mapViewModel.toggleFilterTag(tagId) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 67.dp)   // 헤더 카드 아래 공간 확보
                )


                // 날씨/온도 표시 (왼쪽 하단, my_location 버튼 위)
                WeatherTemperatureDisplay(
                    weather = uiWeather,
                    temperature = uiTemperature,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            bottom = 70.dp + dragHandleHeight // my_location 버튼(40dp) + 간격(14dp) + 기존패딩(16dp)
                        )
                        .offset(y = locationButtonOffsetY)
                )

                // 애니메이션 툴팁 오버레이
                AnimatedMapTooltip(
                    visible = tooltipState.isVisible,
                    content = tooltipState.content?.title ?: "",
                    position = tooltipState.position,
                    type = tooltipState.type
                )


                // 날짜 선택 다이얼로그
                if (showDatePicker) {
                    Dialog(
                        onDismissRequest = { showDatePicker = false }
                    ) {
                        KoreanDatePicker(
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

                if (showRecordDialog) {
                    RecordUploadDialog(
                        isUploading = recordUi.isUploading,
                        onRequestAudioPermission = {
                            shouldStartAfterPermission = true
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onCancel = { showRecordDialog = false },
                        onRegister = { recordedFile ->
                            val pos = myLatLng
                                ?: run {
                                    // 위치 모를 때 한 번 시도 후 안내
                                    fetchMyLocationOnce()
                                    android.widget.Toast.makeText(context, "현재 위치 확인 중입니다. 잠시 후 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                                    return@RecordUploadDialog
                                }

                            recordUploadVm.upload(
                                file = recordedFile,
                                latitude = pos.latitude,
                                longitude = pos.longitude
                            )
                        }
                    )
                }


                // AudioPlayer가 활성화되었을 때 지도 클릭 감지 오버레이
                if (currentPlayingRecord != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                mapViewModel.stopRecord()
                            }
                            .zIndex(999f) // AudioPlayer보다 아래에 있어야 함
                    )
                }

                // 오디오 플레이어 오버레이 - 애니메이션 추가
                AnimatedVisibility(
                    visible = currentPlayingRecord != null,
                    enter = slideInVertically(
                        initialOffsetY = { it }, // 아래에서 위로 슬라이드
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it }, // 위에서 아래로 슬라이드
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    ) + fadeOut(
                        animationSpec = tween(200)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 36.dp) // 바텀 위치 조정
                        .zIndex(1000f) // 최상위에 표시
                ) {
                    currentPlayingRecord?.let { record ->
                        val isMyRecord = mapViewModel.isMyRecord(record, currentUserId)
                        
                        AudioPlayer(
                            recordUrl = record.recordUrl,
                            recordId = record.recordId,
                            authorName = record.author.nickname,
                            createdAt = record.createdAt,
                            onClose = {
                                mapViewModel.stopRecord()
                            },
                            onDelete = if (isMyRecord) {
                                {
                                    // 삭제 확인 다이얼로그 표시
                                    android.app.AlertDialog.Builder(context)
                                        .setTitle("음성 녹음 삭제")
                                        .setMessage("이 음성 녹음을 삭제하시겠습니까?\n삭제된 음성은 복구할 수 없습니다.")
                                        .setPositiveButton("삭제") { _, _ ->
                                            mapViewModel.deleteRecord(
                                                recordId = record.recordId,
                                                onSuccess = {
                                                    android.widget.Toast.makeText(context, "음성 녹음이 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { errorMessage ->
                                                    android.widget.Toast.makeText(context, "삭제 실패: $errorMessage", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                        .setNegativeButton("취소", null)
                                        .show()
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // POI 상세 다이얼로그
        selectedPOI?.let { poi ->
            if (showPOIDialog) {
                POIDetailDialog(
                    poi = poi,
                    isGeneratingSummary = isLoadingPOIDetail,
                    onDismiss = { mapViewModel.dismissPOIDialog() }
                )
            }
        }
    }
}

private fun distanceMeters(a: com.naver.maps.geometry.LatLng, b: com.naver.maps.geometry.LatLng): Float {
    val out = FloatArray(1)
    android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
    return out[0]
}

/**
 * 지도에 표시된 컨텐츠들로부터 날씨 정보를 계산
 */
private fun calculateWeatherInfo(mapContents: List<com.shinhan.campung.data.model.MapContent>): WeatherInfo {
    if (mapContents.isEmpty()) {
        return WeatherInfo(weather = null, temperature = null)
    }

    // 날씨 정보가 있는 컨텐츠들만 필터링
    val contentsWithWeather = mapContents.filter {
        !it.emotionWeather.isNullOrBlank() || it.emotionTemperature != null
    }

    if (contentsWithWeather.isEmpty()) {
        return WeatherInfo(weather = null, temperature = null)
    }

    // 가장 많이 나타나는 날씨 찾기
    val weatherCounts = contentsWithWeather
        .mapNotNull { it.emotionWeather }
        .groupingBy { it }
        .eachCount()

    val mostCommonWeather = weatherCounts.maxByOrNull { it.value }?.key

    // 온도 평균 계산
    val temperatures = contentsWithWeather.mapNotNull { it.emotionTemperature }
    val averageTemperature = if (temperatures.isNotEmpty()) {
        temperatures.average().toInt()
    } else null

    return WeatherInfo(
        weather = mostCommonWeather,
        temperature = averageTemperature
    )
}

/**
 * 날씨 정보 데이터 클래스
 */
private data class WeatherInfo(
    val weather: String?,
    val temperature: Int?
)
private fun normalizeWeather(raw: String?): String? {
    val k = raw?.trim()?.lowercase() ?: return null
    return when (k) {
        "맑음","해","쾌청","sun","clear","fine","sunny" -> "sunny"
        "구름","흐림","흐림많음","cloud","overcast","cloudy","clouds" -> "cloudy"
        "비","소나기","drizzle","rain shower","rainy","rain" -> "rain"
        "천둥","천둥번개","번개","뇌우","thunder","storm","thunderstorm","stormy" -> "thunderstorm"
        else -> null
    }
}