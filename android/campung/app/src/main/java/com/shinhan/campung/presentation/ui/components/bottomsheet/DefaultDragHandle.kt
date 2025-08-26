package com.shinhan.campung.presentation.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 기본 드래그 핸들 UI
 */
@Composable
fun DefaultDragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 40.dp,
    height: Dp = 4.dp,
    color: Color = Color.Gray
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp), // 터치 영역을 위한 최소 높이
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .background(
                    color = color,
                    shape = RoundedCornerShape(height / 2)
                )
                .semantics {
                    contentDescription = "바텀시트 드래그 핸들"
                }
        )
    }
}