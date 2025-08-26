package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.data.model.MapContent
@Composable
fun MapBottomSheetContent(
    contents: List<MapContent>,
    isLoading: Boolean = false,
    isInteractionEnabled: Boolean = true,
    navigationBarHeight: Dp = 0.dp, // 추가된 파라미터
    statusBarHeight: Dp = 0.dp, // 추가된 파라미터
    onContentClick: (MapContent) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // 높이 구성 요소
    val itemHeight = 120.dp
    val padding = 16.dp
    val itemSpacing = 8.dp

    // 네비게이션 바와 상태바를 모두 제외한 사용 가능한 높이
    val availableHeight = screenHeight - navigationBarHeight - statusBarHeight

    val expandedHeight = when (contents.size) {
        0 -> 0.dp
        1 -> itemHeight + (padding * 2)
        2 -> (itemHeight * 2) + itemSpacing + (padding * 2)
        else -> availableHeight * 0.5f  // 실제 사용가능 높이의 50%
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = expandedHeight),  // 최대 높이 제한
        color = Color.White,  // 강제로 흰색 배경
        contentColor = Color.Black  // 텍스트 색상 검은색
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        // 컨텐츠 리스트
        if (isLoading) {
            // 로딩 상태: 즉각적인 피드백 제공
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF0066FF))
                    Text(
                        text = "콘텐츠를 불러오는 중...",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else if (contents.isEmpty()) {
            // 빈 상태: 핸들만 보이고 상호작용 불가
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // 빈 상태 표시 (선택사항)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(padding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(contents) { content ->
                    MapContentItem(
                        content = content,
                        modifier = Modifier.height(itemHeight),
                        onClick = { if (isInteractionEnabled) onContentClick(content) }
                    )
                }
            }
        }
        }
    }
}