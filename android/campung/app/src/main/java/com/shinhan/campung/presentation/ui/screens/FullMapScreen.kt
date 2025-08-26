package com.shinhan.campung.presentation.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.naver.maps.map.widget.LocationButtonView
import com.shinhan.campung.presentation.ui.components.MapBottomSheetContent
import com.shinhan.campung.presentation.viewmodel.MapViewModel

// 커스텀 bottom sheet를 위한 imports
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import kotlin.math.abs

// 커스텀 바텀시트 상태
enum class CustomSheetValue {
    PartiallyExpanded,
    Expanded,
    Hidden
}

@Composable
fun FullMapScreen(
    navController: NavController,
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

    // 화면 크기
    val screenHeight = configuration.screenHeightDp.dp
    val itemHeight = 120.dp
    val padding = 16.dp
    val itemSpacing = 8.dp
    val dragHandleHeight = 30.dp // 드래그 핸들 영역 높이 30dp로 변경
    val sheetHeaderHeight = 48.dp // 기본 시트 헤더 높이

    // MapBottomSheetContent의 실제 패딩들 (추정값이 아닌 실제 컴포넌트 기준)
    val bottomSheetContentPadding = 16.dp // MapBottomSheetContent 내부 패딩
    val bottomSheetItemSpacing = 8.dp // 아이템 간 간격

    // 커스텀 바텀시트 상태
    var currentSheetValue by rememberSaveable { mutableStateOf(CustomSheetValue.PartiallyExpanded) }
    var isDragging by remember { mutableStateOf(false) }

    // Y 위치 계산
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density)
    val statusBarHeight = WindowInsets.statusBars.getTop(density) // 상태바 높이 추가
    val availableHeightPx = screenHeightPx - navigationBarHeight - statusBarHeight // 상태바와 네비게이션 바 모두 제외

    // 디버깅용 로그
    LaunchedEffect(Unit) {
        val navBarHeightDp = with(density) { navigationBarHeight.toDp() }
        val statusBarHeightDp = with(density) { statusBarHeight.toDp() }
        val availableHeightDp = with(density) { availableHeightPx.toDp() }
        Log.d("BottomSheet", "=== 바텀시트 높이 디버깅 ===")
        Log.d("BottomSheet", "Screen height: ${screenHeight}")
        Log.d("BottomSheet", "Status bar height: ${statusBarHeightDp}")
        Log.d("BottomSheet", "Navigation bar height: ${navBarHeightDp}")
        Log.d("BottomSheet", "Available height: ${availableHeightDp}")
        Log.d("BottomSheet", "Drag handle height: ${dragHandleHeight}")
    }

    // 바텀시트 위치 계산 - 로딩 상태 고려 및 정확한 패딩 계산
    val sheetPositions = remember(availableHeightPx, density, bottomSheetContents.size, isLoading) {
        object {
            // 네비게이션 바 위에 드래그 핸들 + 더 큰 여유 공간 노출
            val partiallyExpanded = availableHeightPx - with(density) {
                (dragHandleHeight).toPx() // 네비게이션 바 위에 30dp 여유 공간 (기존 8dp에서 증가)
            }

            val contentHeight = when {
                // 로딩 중일 때는 항상 1개 컨텐츠 크기로 계산 (모든 패딩 포함)
                isLoading -> dragHandleHeight +
                        bottomSheetContentPadding * 2 + // 상하 패딩
                        itemHeight + // 로딩 영역 높이
                        sheetHeaderHeight
                // 로딩 완료 후 실제 컨텐츠 갯수에 따라 계산
                bottomSheetContents.isEmpty() -> dragHandleHeight + sheetHeaderHeight
                bottomSheetContents.size == 1 -> dragHandleHeight +
                        itemHeight +
                        sheetHeaderHeight
                bottomSheetContents.size == 2 -> dragHandleHeight +
                        (itemHeight * 2) +
                        bottomSheetItemSpacing +
                        sheetHeaderHeight
                else -> {
                    val maxHeight = screenHeight * 0.5f // 원래대로 되돌림
                    val calculatedHeight = dragHandleHeight +
                            bottomSheetContentPadding * 2 +
                            (itemHeight * bottomSheetContents.size) +
                            (bottomSheetItemSpacing * (bottomSheetContents.size - 1)) +
                            sheetHeaderHeight
                    minOf(maxHeight, calculatedHeight)
                }
            }
            val calculatedExpanded = availableHeightPx - with(density) { contentHeight.toPx() } // screenHeightPx 대신 availableHeightPx 사용
            // 안전장치: expanded가 partiallyExpanded보다 작아야 함 (Y좌표계에서 위쪽이 작은 값)
            val expanded = calculatedExpanded.coerceAtMost(partiallyExpanded - with(density) { 10.dp.toPx() })
        }
    }

    // 바텀시트 위치 디버깅 로그 (remember 블록 밖에서)
    LaunchedEffect(sheetPositions, bottomSheetContents.size, isLoading) {
        Log.d("BottomSheet", "=== 바텀시트 위치 계산 ===")
        Log.d("BottomSheet", "PartiallyExpanded: ${with(density) { sheetPositions.partiallyExpanded.toDp() }}")
        Log.d("BottomSheet", "Expanded: ${with(density) { sheetPositions.expanded.toDp() }}")
        Log.d("BottomSheet", "Contents size: ${bottomSheetContents.size}, isLoading: $isLoading")
    }

    // Animatable for smooth animation
    val sheetAnimY = remember { Animatable(sheetPositions.partiallyExpanded) }

    // 상태 변경시 애니메이션
    LaunchedEffect(currentSheetValue) {
        val targetY = when (currentSheetValue) {
            CustomSheetValue.PartiallyExpanded -> sheetPositions.partiallyExpanded
            CustomSheetValue.Expanded -> sheetPositions.expanded
            CustomSheetValue.Hidden -> screenHeightPx
        }

        sheetAnimY.animateTo(
            targetValue = targetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // sheetPositions가 변경될 때도 애니메이션 재실행 (로딩 완료 후 크기 조정)
    LaunchedEffect(sheetPositions) {
        val targetY = when (currentSheetValue) {
            CustomSheetValue.PartiallyExpanded -> sheetPositions.partiallyExpanded
            CustomSheetValue.Expanded -> sheetPositions.expanded
            CustomSheetValue.Hidden -> screenHeightPx
        }

        sheetAnimY.animateTo(
            targetValue = targetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // LocationButton 오프셋 계산
    val locationButtonOffsetY = with(density) {
        val currentOffset = (sheetAnimY.value - sheetPositions.partiallyExpanded).coerceAtMost(0f)
        currentOffset.toDp()
    }

    // 빈 상태에서는 확장을 허용하지 않음 (단, 로딩 중에는 허용)
    fun confirmValueChange(targetValue: CustomSheetValue): Boolean {
        return if (bottomSheetContents.isEmpty() && !isLoading) {
            targetValue == CustomSheetValue.PartiallyExpanded
        } else {
            true
        }
    }

    // 드래그 처리
    fun handleDragEnd(velocity: Float, currentPosition: Float) {
        val fastSwipeThreshold = 800f
        val midpoint = (sheetPositions.partiallyExpanded + sheetPositions.expanded) / 2

        val targetState = when {
            // 로딩 중이 아니고 컨텐츠가 없으면 축소 상태만 허용
            bottomSheetContents.isEmpty() && !isLoading -> CustomSheetValue.PartiallyExpanded
            velocity > fastSwipeThreshold -> CustomSheetValue.PartiallyExpanded
            velocity < -fastSwipeThreshold -> CustomSheetValue.Expanded
            currentPosition > midpoint -> CustomSheetValue.PartiallyExpanded
            else -> CustomSheetValue.Expanded
        }

        if (confirmValueChange(targetState)) {
            currentSheetValue = targetState
        }
        isDragging = false
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

    // 위치 권한 및 관련 로직
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

    BackHandler { navController.popBackStack() }

    // 마커 클릭시 자동 확장 - 로딩 상태와 컨텐츠 갯수 모두 고려
    LaunchedEffect(isLoading) {
        // 로딩 시작시 바로 확장
        if (isLoading) {
            currentSheetValue = CustomSheetValue.Expanded
        }
    }

    LaunchedEffect(bottomSheetContents.size) {
        // 로딩이 끝나고 컨텐츠가 업데이트될 때
        if (!isLoading) {
            when {
                bottomSheetContents.isEmpty() -> currentSheetValue = CustomSheetValue.PartiallyExpanded
                bottomSheetContents.size >= 1 -> currentSheetValue = CustomSheetValue.Expanded
            }
        }
    }

    // isBottomSheetExpanded와 동기화
    LaunchedEffect(isBottomSheetExpanded) {
        if (isBottomSheetExpanded && (bottomSheetContents.isNotEmpty() || isLoading)) {
            currentSheetValue = CustomSheetValue.Expanded
        } else if (!isBottomSheetExpanded && bottomSheetContents.isNotEmpty() && !isLoading) {
            currentSheetValue = CustomSheetValue.PartiallyExpanded
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
            // 전체 화면 지도
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

                            // 지도 이동시 바텀시트 축소
                            map.addOnCameraChangeListener { _, _ ->
                                mapViewModel.onMapMove()
                            }

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

            // 커스텀 바텀시트 - 배경을 사용가능한 높이까지만 연장 (상단만 radius)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { availableHeightPx.toDp() }) // screenHeightPx 대신 availableHeightPx 사용
                    .offset(y = with(density) { sheetAnimY.value.toDp() })
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        ) // 상단만 radius, 하단은 직각
                    )
            ) {
                // 실제 바텀시트 컨텐츠
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                ) {
                    Column {
                        // 드래그 핸들 - 더 큰 터치 영역으로 확장
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dragHandleHeight) // 터치 영역 확장
                                .pointerInput(Unit) {
                                    var totalDragAmount = 0f
                                    var dragStartTime = 0L

                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                            totalDragAmount = 0f
                                            dragStartTime = System.currentTimeMillis()
                                        },
                                        onDragEnd = {
                                            val duration = System.currentTimeMillis() - dragStartTime
                                            val velocity =
                                                if (duration > 0) totalDragAmount / duration * 1000 else 0f
                                            handleDragEnd(velocity, sheetAnimY.value)
                                        }
                                    ) { _, dragAmount ->
                                        totalDragAmount += dragAmount.y

                                        // 드래그 중 실시간 업데이트
                                        coroutineScope.launch {
                                            val newY = (sheetAnimY.value + dragAmount.y).coerceIn(
                                                sheetPositions.expanded, // min (위쪽, 작은 값)
                                                sheetPositions.partiallyExpanded // max (아래쪽, 큰 값)
                                            )
                                            sheetAnimY.snapTo(newY)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 드래그 핸들 UI
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        color = Color.Gray,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .semantics {
                                        contentDescription = "바텀시트 드래그 핸들"
                                    }
                            )
                        }

                        // 바텀시트 콘텐츠
                        MapBottomSheetContent(
                            contents = bottomSheetContents,
                            isLoading = isLoading,
                            isInteractionEnabled = bottomSheetContents.isNotEmpty() || isLoading,
                            navigationBarHeight = with(density) { navigationBarHeight.toDp() },
                            statusBarHeight = with(density) { statusBarHeight.toDp() }, // 상태바 높이도 전달
                            onContentClick = { content ->
                                // TODO: 컨텐츠 상세 화면으로 이동
                            }
                        )
                    }
                }
            }
        }
    }
}

// 예시 마커 추가 함수
private fun addSampleMarkers(map: NaverMap, mapViewModel: MapViewModel) {
    val sampleMarkers = listOf(
        Triple(37.5665, 126.9780, listOf(1L, 2L)),
        Triple(37.5640, 126.9750, listOf(3L)),
        Triple(37.5660, 126.9820, listOf(4L, 5L, 6L, 1L, 2L))
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