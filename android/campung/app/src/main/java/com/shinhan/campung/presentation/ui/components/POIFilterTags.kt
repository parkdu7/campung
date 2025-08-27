package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun POIFilterTags(
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val poiCategories = listOf(
        POICategory("전체", null, "🏛️"),
        POICategory("도서관", "LIBRARY", "📚"),
        POICategory("식당", "RESTAURANT", "🍽️"),
        POICategory("카페", "CAFE", "☕"),
        POICategory("기숙사", "DORMITORY", "🏠"),
        POICategory("푸드트럭", "FOOD_TRUCK", "🚚"),
        POICategory("행사", "EVENT", "🎉"),
        POICategory("대학건물", "UNIVERSITY_BUILDING", "🏫")
    )
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(poiCategories) { category ->
            POIFilterChip(
                category = category,
                isSelected = selectedCategory == category.id,
                onClick = { onCategoryClick(category.id) }
            )
        }
    }
}

@Composable
private fun POIFilterChip(
    category: POICategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = category.emoji,
            fontSize = 14.sp
        )
        
        Text(
            text = category.displayName,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private data class POICategory(
    val displayName: String,
    val id: String?, // null이면 전체 카테고리
    val emoji: String
)