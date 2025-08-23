package com.shinhan.campung.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.presentation.ui.components.*
import com.shinhan.campung.presentation.ui.theme.CampusBackground
import com.shinhan.campung.presentation.ui.theme.CampusPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "알림")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "메뉴")
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