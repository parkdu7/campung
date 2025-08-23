package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.presentation.ui.theme.CampusPrimary

@Composable
fun ScheduleSection() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "오늘의 일정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    TextButton(
                        onClick = { selectedTab = 0 },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (selectedTab == 0) CampusPrimary else Color.Gray
                        )
                    ) {
                        Text("오늘")
                    }
                    
                    IconButton(onClick = { }) {
                        Text("•••", color = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 일정이 없을 때 표시
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "오늘은 일정이 없습니다",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}