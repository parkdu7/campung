package com.shinhan.campung.presentation.ui.components

import androidx.compose.ui.geometry.Offset
import com.shinhan.campung.data.model.MapContent

data class TooltipState(
    val isVisible: Boolean = false,
    val content: MapContent? = null,
    val position: Offset = Offset.Zero,
    val type: TooltipType = TooltipType.CLICK
)

enum class TooltipType {
    CLICK,   // 클릭시 나타나는 툴팁 (진한 스타일)
    FOCUS    // 포커스시 나타나는 툴팁 (연한 스타일)
}