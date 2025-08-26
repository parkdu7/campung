package com.shinhan.campung.data.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.shinhan.campung.R

data class FilterTag(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    val backgroundColor: Color,
    val textColor: Color = Color.White
)

object FilterTags {
    val HOT = FilterTag(
        id = "hot",
        name = "인기",
        iconRes = R.drawable.hot_icon,
        backgroundColor = Color(0xFFFF6B6B) // 빨간색
    )
    
    val PROMOTION = FilterTag(
        id = "promotion", 
        name = "홍보",
        iconRes = R.drawable.promotion_icon,
        backgroundColor = Color(0xFFFF8C42) // 주황색
    )
    
    val INFO = FilterTag(
        id = "info",
        name = "정보", 
        iconRes = R.drawable.info_icon,
        backgroundColor = Color(0xFF4ECDC4) // 청록색
    )
    
    val MARKET = FilterTag(
        id = "market",
        name = "장터",
        iconRes = R.drawable.store_icon,
        backgroundColor = Color(0xFF9B59B6) // 보라색
    )

    val FREE = FilterTag(
        id = "free",

        name = "자유",
        iconRes = R.drawable.free_icon,
        backgroundColor = Color(0xFFE74C3C) // 진한 빨간색
    )

    
    val ALL_TAGS = listOf(HOT, PROMOTION, INFO, MARKET, FREE)
}