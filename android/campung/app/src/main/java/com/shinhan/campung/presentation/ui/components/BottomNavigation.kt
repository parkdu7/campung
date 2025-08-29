package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import com.shinhan.campung.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.presentation.ui.theme.CampusAccent
import com.shinhan.campung.presentation.ui.theme.CampusPrimary

@Composable
fun BottomNavigation() {
    var selectedTab by remember { mutableStateOf(0) }
    
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Gray
    ) {
        NavigationBarItem(
            icon = { 
                Icon(
                    painter = painterResource(id = R.drawable.outline_school_24),
                    contentDescription = "캠퍼스",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "학사",
                    fontSize = 12.sp
                )
            },
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CampusPrimary,
                selectedTextColor = CampusPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            icon = { 
                Box {
                    Icon(
                        painter = painterResource(id = R.drawable.gifts_24),
                        contentDescription = "혜택",
                        modifier = Modifier.size(24.dp)
                    )
                    // NEW 뱃지
                    if (true) { // 새로운 혜택이 있을 때
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-15).dp)
                                .size(35.dp, 18.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CampusAccent)
                        ) {
                            Text(
                                "NEW",
                                modifier = Modifier.align(Alignment.Center)
                                    .offset(y=((-3).dp)),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            label = { 
                Text(
                    "혜택",
                    fontSize = 12.sp
                )
            },
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CampusPrimary,
                selectedTextColor = CampusPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            icon = { 
                Icon(
                    painter = painterResource(id = R.drawable.outline_menu_24),
                    contentDescription = "캠퍼스 라이프",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "전체 메뉴",
                    fontSize = 12.sp
                )
            },
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CampusPrimary,
                selectedTextColor = CampusPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}