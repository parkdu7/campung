package com.shinhan.campung.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.data.remote.response.MapContent

@Composable
fun MarkerTooltip(
    mapContent: MapContent?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = mapContent != null,
        enter = fadeIn(animationSpec = tween(200)) + 
                scaleIn(
                    animationSpec = tween(200),
                    initialScale = 0.3f
                ),
        exit = fadeOut(animationSpec = tween(150)) +
               scaleOut(
                   animationSpec = tween(150),
                   targetScale = 0.3f
               ),
        modifier = modifier
    ) {
        mapContent?.let { content ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 말풍선 본체
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = content.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 말풍선 꼬리 (삼각형)
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 6.dp)
                        .offset(y = (-1).dp)
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) { 
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.5f, size.height)
                            lineTo(size.width * 0.2f, 0f)
                            lineTo(size.width * 0.8f, 0f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}