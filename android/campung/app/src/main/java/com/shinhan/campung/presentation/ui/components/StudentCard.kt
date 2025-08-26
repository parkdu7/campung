package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.presentation.ui.theme.CampusPrimary
import com.shinhan.campung.presentation.ui.theme.CampusSecondary

@Composable
fun StudentCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            CampusPrimary,
                            CampusSecondary
                        )
                    )
                )
                .padding(40.dp),
//            contentAlignment = Alignment.Center // 박스 안 전체 중앙정렬
        ) {
            Column {
                Text(
                    "전자확인증",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 프로필 이미지 자리 (간단한 캐릭터로 표현)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawStudentAvatar()
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            "컴퓨터공학과, 재학 3학년",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "김신한 (2021001)",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // QR 코드 버튼 영역
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally) // 전체 Column에서 중앙정렬
                        .fillMaxWidth()                      // 가로 전체 차지
                        .clip(RoundedCornerShape(10.dp)) // 둥근 모서리
                        .background(Color.White.copy(alpha = 0.2f)) // 배경
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox, // androidx.compose.material.icons.filled.QrCode 사용
                        contentDescription = "QR",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "QR",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

            }
        }
    }
}

// 학생 아바타 그리기 (간단한 표현)
fun DrawScope.drawStudentAvatar() {
    // 간단한 아바타 표현
    drawCircle(
        color = Color(0xFF4A9EAE),
        radius = size.minDimension / 3
    )
    // 얼굴 표현
    drawCircle(
        color = Color.White,
        radius = size.minDimension / 8,
        center = androidx.compose.ui.geometry.Offset(
            size.width * 0.4f,
            size.height * 0.4f
        )
    )
    drawCircle(
        color = Color.White,
        radius = size.minDimension / 8,
        center = androidx.compose.ui.geometry.Offset(
            size.width * 0.6f,
            size.height * 0.4f
        )
    )
}

@Composable
fun QRCodePlaceholder() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = size.width / 10
        for (i in 0..9) {
            for (j in 0..9) {
                if ((i + j) % 2 == 0 || (i * j) % 3 == 0) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(i * cellSize, j * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}