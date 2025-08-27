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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.shinhan.campung.presentation.ui.components.DatePickerDialog
import com.shinhan.campung.data.model.MapContent
import android.util.Log
import com.shinhan.campung.navigation.Route
import com.shinhan.campung.presentation.ui.components.MapBottomSheetContent
import com.shinhan.campung.presentation.ui.components.AnimatedMapTooltip

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.shinhan.campung.R

// 새로운 바텀시트 컴포넌트 imports
import com.shinhan.campung.presentation.ui.components.bottomsheet.*

@Composable
fun FullMapScreen(
    navController: NavController,
    mapView: MapView, // 외부에서 주입받음
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel states
    val bottomSheetContents by mapViewModel.bottomSheetContents.collectAsState()
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

    // 동적 컨텐츠 높이 계산 (기존 로직과 동일)
    val dynamicContentHeight = remember(bottomSheetContents.size, isLoading) {
        when {
            isLoading -> dragHandleHeight + padding * 2 + itemHeight
            bottomSheetContents.isEmpty() -> dragHandleHeight
            bottomSheetContents.size == 1 -> dragHandleHeight + itemHeight + padding * 2
            bottomSheetContents.size == 2 -> dragHandleHeight + (itemHeight * 2) + itemSpacing + padding * 2
            else -> {
                val maxHeight = screenHeight * 0.5f
                val calculatedHeight = dragHandleHeight + padding * 2 + (itemHeight * bottomSheetContents.size) + (itemSpacing * (bottomSheetContents.size - 1))
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

    // 지도 설정
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
    var mapViewportManager by remember { mutableStateOf<com.shinhan.campung.presentation.ui.map.MapViewportManager?>(null) }
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

            // 초기 로드시에도 화면 영역 기반 반경 계산 사용
            naverMapRef?.let { map ->
                val radius = com.shinhan.campung.presentation.ui.map.MapBoundsCalculator.calculateVisibleRadius(map)
                mapViewModel.loadMapContentsWithCalculatedRadius(
                    latitude = pos.latitude,
                    longitude = pos.longitude,
                    radius = radius
                )
            } ?: run {
                // NaverMap이 아직 준비되지 않았으면 기본 방식 사용
                mapViewModel.loadMapContents(
                    latitude = pos.latitude,
                    longitude = pos.longitude
                )
            }
        }
    }

    // 카메라 이동시 툴팁 위치 업데이트
    LaunchedEffect(naverMapRef) {
        naverMapRef?.let { map ->
            map.addOnCameraChangeListener { reason, animated ->
                // 툴팁이 표시 중일 때만 위치 업데이트
                if (tooltipState.isVisible && tooltipState.content != null) {
                    val content = tooltipState.content!!
                    val latLng = com.naver.maps.geometry.LatLng(content.location.latitude, content.location.longitude)
                    val screenPoint = map.projection.toScreenLocation(latLng)
                    val newPosition = androidx.compose.ui.geometry.Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                    mapViewModel.updateTooltipPosition(newPosition)
                }
            }
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

    // 뒤로가기 버튼 처리
    BackHandler {
        if (mapViewModel.selectedMarker != null || clusterManager?.selectedClusterMarker != null) {
            // 마커나 클러스터가 선택되어 있으면 선택 해제
            mapViewModel.clearSelectedMarker()
            clusterManager?.clearSelection()
        } else {
            // 아무것도 선택되어 있지 않으면 화면 나가기
            navController.popBackStack()
        }
    }

    // 디버깅: 초기 상태 확인
    LaunchedEffect(Unit) {
        Log.d("BottomSheetDebug", "=== FullMapScreen 초기화 ===")
        Log.d("BottomSheetDebug", "초기 bottomSheetContents.size: ${bottomSheetContents.size}")
        Log.d("BottomSheetDebug", "초기 isLoading: $isLoading")
        Log.d("BottomSheetDebug", "초기 isBottomSheetExpanded: $isBottomSheetExpanded")
    }

    // 바텀시트 상태 실시간 추적 - 사용자가 직접 드래그했을 때도 ViewModel에 반영
    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }
            .collect { currentValue ->
                Log.d("BottomSheetDebug", "바텀시트 상태 변화 감지: $currentValue")
                val isExpanded = currentValue == BottomSheetValue.Expanded

                // ViewModel의 상태와 실제 바텀시트 상태가 다를 때만 업데이트
                if (isBottomSheetExpanded != isExpanded) {
                    Log.d("BottomSheetDebug", "ViewModel 상태 업데이트: $isBottomSheetExpanded -> $isExpanded")
                    mapViewModel.updateBottomSheetExpanded(isExpanded)
                }
            }
    }

    // 상태 변화 모니터링
    LaunchedEffect(bottomSheetContents.size, isLoading, isBottomSheetExpanded) {
        Log.d("BottomSheetDebug", "=== 상태 변화 감지 ===")
        Log.d("BottomSheetDebug", "bottomSheetContents.size: ${bottomSheetContents.size}")
        Log.d("BottomSheetDebug", "bottomSheetContents: $bottomSheetContents")
        Log.d("BottomSheetDebug", "isLoading: $isLoading")
        Log.d("BottomSheetDebug", "isBottomSheetExpanded: $isBottomSheetExpanded")
        Log.d("BottomSheetDebug", "bottomSheetState.currentValue: ${bottomSheetState.currentValue}")
    }

    // 마커 클릭시 자동 확장 - SideEffect로 즉시 반응 (단, 실제 마커 선택이 있을 때만)
    if (isLoading && mapViewModel.selectedMarkerId.collectAsState().value != null) {
        SideEffect {
            Log.d("BottomSheetDebug", "SideEffect: isLoading=true, selectedMarkerId 있음, 바텀시트 확장 중")
            coroutineScope.launch {
                bottomSheetState.snapTo(BottomSheetValue.Expanded)
            }
        }
    } else if (isLoading) {
        Log.d("BottomSheetDebug", "SideEffect: isLoading=true이지만 selectedMarkerId 없음, 바텀시트 확장 안함")
    }

    LaunchedEffect(bottomSheetContents.size) {
        Log.d("BottomSheetDebug", "LaunchedEffect(bottomSheetContents.size): ${bottomSheetContents.size}")
        Log.d("BottomSheetDebug", "현재 isLoading: $isLoading")
        
        // 로딩이 끝나고 컨텐츠가 업데이트될 때
        if (!isLoading) {
            when {
                bottomSheetContents.isEmpty() -> {
                    Log.d("BottomSheetDebug", "컨텐츠 없음 -> PartiallyExpanded")
                    bottomSheetState.snapTo(BottomSheetValue.PartiallyExpanded)
                }
                bottomSheetContents.size >= 1 -> {
                    Log.d("BottomSheetDebug", "컨텐츠 ${bottomSheetContents.size}개 -> Expanded")
                    bottomSheetState.snapTo(BottomSheetValue.Expanded)
                }
            }
        } else {
            Log.d("BottomSheetDebug", "로딩 중이므로 바텀시트 상태 변경 안함")
        }
    }

    // isBottomSheetExpanded와 동기화 - 지도 드래그시에는 부드러운 애니메이션 적용
    LaunchedEffect(isBottomSheetExpanded) {
        Log.d("BottomSheetDebug", "LaunchedEffect(isBottomSheetExpanded): $isBottomSheetExpanded")
        Log.d("BottomSheetDebug", "bottomSheetContents.isEmpty(): ${bottomSheetContents.isEmpty()}")
        Log.d("BottomSheetDebug", "isLoading: $isLoading")
        
        if (isBottomSheetExpanded && (bottomSheetContents.isNotEmpty() || isLoading)) {
            Log.d("BottomSheetDebug", "isBottomSheetExpanded=true -> snapTo Expanded")
            bottomSheetState.snapTo(BottomSheetValue.Expanded) // 확장시에는 즉시
        } else if (!isBottomSheetExpanded && bottomSheetContents.isNotEmpty() && !isLoading) {
            Log.d("BottomSheetDebug", "isBottomSheetExpanded=false -> animateTo PartiallyExpanded")
            bottomSheetState.animateTo(BottomSheetValue.PartiallyExpanded) // 축소시에는 애니메이션
        } else {
            Log.d("BottomSheetDebug", "isBottomSheetExpanded 동기화 조건 미충족")
        }
    }

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

                                clusterManager =
                                    clusterManagerInitializer.createClusterManager(map) { centerContent ->
                                        highlightedContent = centerContent
                                    }

                            // 기존 카메라 리스너 (마커 중심점 관리)
                                mapCameraListener = MapCameraListener(mapViewModel, clusterManager)
                                map.addOnCameraChangeListener(mapCameraListener!!.createCameraChangeListener())

                            // 새로운 뷰포트 관리자 (화면 영역 기반 데이터 로드)
                            mapViewportManager = com.shinhan.campung.presentation.ui.map.MapViewportManager(mapViewModel, coroutineScope).apply {
                                setNaverMap(map) // NaverMap 참조 설정
                            }
                            
                            // 뷰포트 매니저의 카메라 리스너 추가
                            map.addOnCameraChangeListener(mapViewportManager!!.createCameraChangeListener())

                                // 지도 클릭 시 마커 및 클러스터 선택 해제
                                map.setOnMapClickListener { _, _ ->
                                    if (mapViewModel.selectedMarker != null || clusterManager?.selectedClusterMarker != null) {
                                        mapViewModel.clearSelectedMarker()
                                        clusterManager?.clearSelection()
                                    }
                                }
                            }
                        } else {
                            naverMapRef?.let { map ->
                                mapInitializer.setupLocationOverlay(map, hasPermission, myLatLng)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 뒤로가기 버튼
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }

                // LocationButton - 바텀시트와 함께 움직임
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            bottom = 16.dp + dragHandleHeight // 바텀시트 드래그 핸들 높이(30dp)만큼 위로
                        )
                        .offset(y = locationButtonOffsetY)
                ) {
                    AndroidView(
                        factory = { ctx -> LocationButtonView(ctx) },
                        update = { btn ->
                            naverMapRef?.let { btn.map = it }
                        }
                    )

                    // 클릭 오버레이
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
                                        locationPermissionManager.requestLocationPermission(
                                            permissionLauncher
                                        )
                                    }
                                }
                            }
                    )
                }

                // 플로팅 버튼 상태 관리
                var isFabExpanded by remember { mutableStateOf(false) }

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
                                            // TODO: 펜/그리기 기능 구현
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.btn_post),
                                        contentDescription = "글쓰기",
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

                        // 메인 버튼 (+ 또는 X) - 가장 아래
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isFabExpanded) 45f else 0f,
                            animationSpec = tween(300),
                            label = "fab_rotation"
                        )

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
                                painter = painterResource(R.drawable.btn_add),
                                contentDescription = if (isFabExpanded) "메뉴 닫기" else "메뉴 열기",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotationAngle) // 45도 회전 애니메이션
                            )
                        }
                    }
                }

                // 새로운 바텀시트 컴포넌트 사용
                MapDraggableBottomSheet(
                    state = bottomSheetState,
                    screenHeight = screenHeight,
                    availableHeight = availableHeight,
                    contentHeight = dynamicContentHeight,
                    dragHandleHeight = dragHandleHeight
                ) {
                    // 바텀시트 콘텐츠
                    MapBottomSheetContent(
                        contents = bottomSheetContents,
                        isLoading = isLoading,
                        isInteractionEnabled = bottomSheetContents.isNotEmpty() || isLoading,
                        navigationBarHeight = with(density) { navigationBarHeight.toDp() },
                        statusBarHeight = with(density) { statusBarHeight.toDp() },
                        onContentClick = { content ->
                            // TODO: 컨텐츠 상세 화면으로 이동
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
                
                // 날씨/온도 표시 (오른쪽 상단, 필터 태그 아래)
                // 표시
                WeatherTemperatureDisplay(
                    weather = uiWeather,
                    temperature = uiTemperature,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 110.dp, end = 8.dp)
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
    }
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