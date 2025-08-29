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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    val elevation = if (isSelected) 8.dp else 4.dp

    val context = LocalContext.current
    // ✅ 선택 시: 현재 아이콘 이름 + "2" 를 drawable에서 찾고, 없으면 원본으로 fallback
    val iconResToUse = remember(tag.iconRes, isSelected) {
        if (isSelected) {
            val baseName = context.resources.getResourceEntryName(tag.iconRes)
            val selectedId = context.resources.getIdentifier(
                baseName + "2", "drawable", context.packageName
            )
            if (selectedId != 0) selectedId else tag.iconRes
        } else {
            tag.iconRes
        }
    }

    Row(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResToUse),
            contentDescription = tag.name,
            tint = Color.Unspecified,          // PNG/벡터 원본 색 유지
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