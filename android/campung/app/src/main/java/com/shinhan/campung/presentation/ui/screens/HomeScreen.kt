package com.shinhan.campung.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.shinhan.campung.presentation.viewmodel.NewPostViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shinhan.campung.presentation.ui.components.*
import com.shinhan.campung.presentation.ui.theme.CampusBackground
import com.shinhan.campung.presentation.ui.theme.CampusPrimary
import com.shinhan.campung.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onLoggedOut: () -> Unit
) {
    val vm: HomeViewModel = hiltViewModel()
    val newPostVm: NewPostViewModel = hiltViewModel()
    
    // 새 게시글 서비스 시작
    LaunchedEffect(Unit) {
        newPostVm.startNewPostService()
    }
    
    // 스낵바 상태
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 새 게시글 알림 처리
    LaunchedEffect(newPostVm.showNotification.value) {
        if (newPostVm.showNotification.value) {
            val result = snackbarHostState.showSnackbar(
                message = "근처에 새 게시글이 올라왔어요!",
                actionLabel = "확인",
                duration = SnackbarDuration.Short
            )
            newPostVm.dismissNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "신한캠퍼스",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CampusPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SHINHAN CAMPUS",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("notification") }) {
                        Icon(Icons.Default.Notifications, contentDescription = "알림")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "메뉴")
                    }
                    IconButton(
                        onClick = { vm.logout(onLoggedOut) },         // 로그아웃
                        enabled = !vm.loading.value
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "로그아웃")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CampusBackground)
                .verticalScroll(rememberScrollState())
        ) {
            // 학생증 카드
            StudentCard()

            // 맵 카드
            CampusMapCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                onExpandRequest = { navController.navigate("map/full") }
            )
            Spacer(Modifier.height(12.dp))

            // 환급금 안내 배너
            RefundBanner()
            
            // 일정 섹션
            ScheduleSection()
            
            // 캘린더
            CalendarView()
            
            Spacer(modifier = Modifier.height(16.dp))
        }




    }
}