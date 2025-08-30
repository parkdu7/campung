package com.shinhan.campung.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    
    // 배경 및 텍스트 색상 명시 설정
    background = Color(0xFFFFFBFE),        // 흰색 배경
    surface = Color(0xFFFFFBFE),           // 흰색 서피스  
    onPrimary = Color.White,               // Primary 위 텍스트
    onSecondary = Color.White,             // Secondary 위 텍스트
    onTertiary = Color.White,              // Tertiary 위 텍스트
    onBackground = Color.Black,            // 배경 위 텍스트 → 검은색
    onSurface = Color.Black,               // 서피스 위 텍스트 → 검은색
)

@Composable
fun CampungTheme(
    darkTheme: Boolean = false,  // 항상 false로 고정 (라이트모드 전용)
    content: @Composable () -> Unit
) {
    // 항상 LightColorScheme 사용
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}