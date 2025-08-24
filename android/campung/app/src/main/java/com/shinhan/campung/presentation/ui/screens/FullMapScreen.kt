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
import androidx.core.content.ContextCompat
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

@Composable
fun FullMapScreen(
    navController: NavController
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
                                isLocationButtonEnabled = false // 자체 LocationButtonView 사용
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

            // 상단 좌측: 뒤로가기(아이콘)
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
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
