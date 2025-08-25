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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.widget.LocationButtonView
import com.shinhan.campung.presentation.ui.components.MapBottomSheetContent
import com.shinhan.campung.presentation.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullMapScreen(
    navController: NavController,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configuration = LocalConfiguration.current
    
    // ViewModel states
    val bottomSheetContents by mapViewModel.bottomSheetContents.collectAsState()
    val isBottomSheetExpanded by mapViewModel.isBottomSheetExpanded.collectAsState()
    
    // BottomSheetScaffold state  
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            confirmValueChange = { targetValue ->
                // 빈 상태에서는 확장을 허용하지 않음
                if (bottomSheetContents.isEmpty()) {
                    targetValue == SheetValue.PartiallyExpanded
                } else {
                    true
                }
            }
        )
    )
    val screenHeight = configuration.screenHeightDp.dp
    
    // 높이 구성 요소 (기본 핸들은 BottomSheetScaffold에서 자동 처리)
    val itemHeight = 120.dp
    val padding = 16.dp
    val itemSpacing = 8.dp
    
    // Peek 높이 (네비게이션 바 위에 핸들이 보이도록)
    val density = LocalDensity.current
    val navigationBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val peekHeight = 24.dp + navigationBarHeight  // 기본 핸들 + 네비게이션 바 높이

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

    // 내 위치 찍히면 카메라 이동
    LaunchedEffect(myLatLng, naverMapRef) {
        val map = naverMapRef
        val pos = myLatLng
        if (map != null && pos != null) {
            map.moveCamera(CameraUpdate.scrollAndZoomTo(pos, 16.0))
            map.locationOverlay.isVisible = true
            map.locationOverlay.position = pos
        }
    }

    // 하드웨어/소프트웨어 뒤로가기 모두 홈으로
    BackHandler { navController.popBackStack() }
    
    // 마커 클릭시 자동 확장 제어
    LaunchedEffect(bottomSheetContents.size) {
        when (bottomSheetContents.size) {
            0 -> {
                // 빈 상태: 핸들만 보이고 드래그 불가
                scaffoldState.bottomSheetState.partialExpand()
            }
            1 -> scaffoldState.bottomSheetState.expand()  // 1개: 1개 보이게 확장
            2 -> scaffoldState.bottomSheetState.expand()  // 2개: 2개 다 보이게
            else -> scaffoldState.bottomSheetState.expand()  // 3개+: 50% 최대 상태
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetContainerColor = Color.White,  // 바텀시트 배경색을 흰색으로
        sheetContentColor = Color.Black,    // 바텀시트 텍스트 색상을 검은색으로
        sheetContent = {
            MapBottomSheetContent(
                contents = bottomSheetContents,
                isInteractionEnabled = bottomSheetContents.isNotEmpty(),
                onContentClick = { content ->
                    // TODO: 컨텐츠 상세 화면으로 이동
                    // navController.navigate("content_detail/${content.contentId}")
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 시스템 바 영역을 피하도록 패딩 적용
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
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
                                isLocationButtonEnabled = false // 자체 LocationButtonView 사용
                            }
                            
                            // 지도 이동시 바텀시트 축소
                            map.addOnCameraChangeListener { _, _ ->
                                mapViewModel.onMapMove()
                            }
                            
                            // 예시 마커 추가 (실제 데이터는 WebSocket에서 받아올 예정)
                            addSampleMarkers(map, mapViewModel)
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

            // 상단 좌측: 뒤로가기(아이콘)
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                AndroidView(
                    factory = { ctx -> LocationButtonView(ctx) },
                    update = { btn ->
                        // 모양/애니메이션은 유지하고, 실제 기능은 오버레이에서 처리
                        naverMapRef?.let { btn.map = it }
                    }
                    // size는 기본 wrapContent. 필요시 .size(48.dp) 등 지정 가능
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
        }
    }
}

// 예시 마커 추가 함수 (임시)
private fun addSampleMarkers(map: NaverMap, mapViewModel: MapViewModel) {
    // 테스트용 마커들
    val sampleMarkers = listOf(
        Triple(37.5665, 126.9780, listOf(1L, 2L)), // 서울시청 근처
        Triple(37.5640, 126.9750, listOf(3L)), // 을지로 근처
        Triple(37.5660, 126.9820, listOf(4L, 5L, 6L)) // 종로 근처
    )
    
    sampleMarkers.forEachIndexed { index, (lat, lng, contentIds) ->
        val marker = Marker()
        marker.position = LatLng(lat, lng)
        marker.map = map
        marker.captionText = "마커 ${index + 1}"
        
        marker.setOnClickListener {
            mapViewModel.onMarkerClick(
                contentId = contentIds.first(),
                associatedContentIds = contentIds
            )
            true
        }
    }
}
