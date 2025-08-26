package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.shinhan.campung.R
import com.shinhan.campung.data.model.ContentCategory
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.presentation.utils.TimeFormatter

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MapContentItem(
    content: MapContent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이미지 - 정사각형
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                content.thumbnailUrl?.let { url ->
                    GlideImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // 컨텐츠 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 제목과 카테고리
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = content.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    
                    Icon(
                        painter = painterResource(id = content.category.iconRes),
                        contentDescription = content.category.displayName,
                        modifier = Modifier.size(20.dp),
                        tint = getCategoryColor(content.category)
                    )
                }
                
                // 본문 내용
                Text(
                    text = content.content,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                // Spacer로 하단에 메타정보 고정
                Spacer(modifier = Modifier.weight(1f))
                
                // 작성자 정보와 좋아요/댓글
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 작성자 정보와 시간
                    Text(
                        text = "${content.authorNickname} · ${TimeFormatter.formatRelativeTime(content.createdAt)}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // 좋아요, 댓글
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_heart),
                                contentDescription = "좋아요",
                                modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = content.likeCount.toString(),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_comment),
                                contentDescription = "댓글",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = content.commentCount.toString(),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryColor(category: ContentCategory): Color {
    return when (category) {
        ContentCategory.INFO -> Color(0xFF2196F3)
        ContentCategory.PROMOTION -> Color(0xFFFF9800)
        ContentCategory.FREE -> Color(0xFF4CAF50)
        ContentCategory.MARKET -> Color(0xFF9C27B0)
        ContentCategory.HOT -> Color(0xFFF44336)
    }
}