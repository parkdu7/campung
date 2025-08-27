package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.data.model.FilterTag
import com.shinhan.campung.data.model.FilterTags

@Composable
fun HorizontalFilterTags(
    selectedTags: Set<String> = emptySet(),
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(FilterTags.ALL_TAGS) { tag ->
            FilterTagItem(
                tag = tag,
                isSelected = selectedTags.contains(tag.id),
                onClick = { onTagClick(tag.id) }
            )
        }
    }
}

@Composable
private fun FilterTagItem(
    tag: FilterTag,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val bg = if (isSelected) tag.backgroundColor else Color.White
    val fg = if (isSelected) tag.textColor else Color(0xFF1C1C1E)
    val elevation = if (isSelected) 8.dp else 4.dp    // 선택 시 살짝 더 깊은 그림자

    Row(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false) // ✨ 그림자
            .clip(shape)                                                // 둥근 모서리 내용만 잘라주기
            .background(bg)                                             // 배경색 (비선택=흰색)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = tag.iconRes),
            contentDescription = tag.name,
            tint = fg,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = tag.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}
