package com.shinhan.campung.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.R
import kotlin.math.roundToInt
import android.util.Log

/**
 * 날씨와 온도 정보를 표시하는 컴포넌트
 * @param weather 날씨 상태 (sunny, cloudy, rainy, snowy 등)
 * @param temperature 온도 (0-100 범위)
 * @param modifier Modifier
 */
@Composable
fun WeatherTemperatureDisplay(
    weather: String?,
    temperature: Int?,
    modifier: Modifier = Modifier
) {
    // 실제 받아온 데이터 로그 출력
    Log.d("WeatherTemperatureDisplay", "🌟 받은 데이터 - weather: '$weather', temperature: $temperature")
    // 실제 받아온 데이터 로그 출력
    Log.d("WeatherTemperatureDisplay", "Raw data - weather: $weather, temperature: $temperature")
    Column(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // 캠퍼스 라벨
        Text(
            text = "캠퍼스현황",
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black.copy(alpha = 0.7f)
        )
        
        // 날씨 이미지
        WeatherIcon(
            weather = weather,
            modifier = Modifier.size(28.dp)
        )
        
        // 온도 프로그레스 바
        TemperatureProgressBar(
            temperature = temperature,
            modifier = Modifier.width(28.dp) // 날씨 아이콘과 같은 너비
        )
    }
}

/**
 * 날씨 아이콘 컴포넌트
 */
@Composable
private fun WeatherIcon(
    weather: String?,
    modifier: Modifier = Modifier
) {
    val weatherDrawable = getWeatherDrawable(weather)
    
    // drawable 리소스로 날씨 아이콘 표시
    Icon(
        painter = painterResource(id = weatherDrawable),
        contentDescription = "날씨 아이콘",
        modifier = modifier,
        tint = Color.Unspecified // 원본 색상 유지
    )
}

/**
 * 온도 프로그레스 바 컴포넌트 (세로 정렬, 온도는 아래)
 */
@Composable
private fun TemperatureProgressBar(
    temperature: Int?,
    modifier: Modifier = Modifier
) {
    val safeTemperature = temperature?.coerceIn(0, 100) ?: 0
    val progress by animateFloatAsState(
        targetValue = safeTemperature / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "temperature_progress"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 온도계 이미지 + 내부 게이지
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(50.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Icon(
                painter = painterResource(id = R.drawable.thermometer),
                contentDescription = "온도계",
                modifier = Modifier.fillMaxSize(),
                tint = Color.Gray.copy(alpha = 0.7f)
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(progress)
                    .offset(y = (-12).dp) // 온도계 하단 구멍 위치 보정 (더 컴팩트하게)
                    .background(
                        color = getTemperatureColor(safeTemperature),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // 온도 텍스트 - 위로 살짝 끌어올려 '완전히 딱 붙게'
        Text(
            text = "${safeTemperature}°",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = getTemperatureColor(safeTemperature),
            modifier = Modifier.offset(x = (1).dp, y = (-12).dp) // 더 컴팩트하게 조정
        )
    }
}


/**
 * 날씨 상태에 따른 drawable 리소스 반환
 */
// WeatherTemperatureDisplay.kt
private fun getWeatherDrawable(weather: String?): Int {
    return when (weather?.trim()?.lowercase()) {
        "sunny","clear" -> R.drawable.weather_sunny
        "cloudy","clouds","구름","흐림","흐림많음" -> R.drawable.weather_cloudy
        "rainy","rain","비","소나기","drizzle" -> R.drawable.weather_rainy
        "stormy","thunderstorm","천둥","천둥번개","번개","뇌우" -> R.drawable.weather_stormy
        else -> R.drawable.weather_normal
    }
}

/**
 * 온도에 따른 색상 반환
 */
private fun getTemperatureColor(temperature: Int): Color {
    return when {
        temperature <= 20 -> Color(0xFF64B5F6) // 파랑 (차가움)
        temperature <= 40 -> Color(0xFF81C784) // 초록 (시원함)  
        temperature <= 60 -> Color(0xFFFFB74D) // 주황 (따뜻함)
        temperature <= 80 -> Color(0xFFFF8A65) // 빨강-주황 (더움)
        else -> Color(0xFFE53935) // 빨강 (매우 더움)
    }
}