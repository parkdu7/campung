package com.shinhan.campung.presentation.ui.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shinhan.campung.data.model.Author
import com.shinhan.campung.presentation.utils.TimeFormatter
import java.time.LocalDateTime

@Composable
fun AuthorSection(
    author: Author,
    createdAt: LocalDateTime,
    modifier: Modifier = Modifier.padding(start = 15.dp, bottom = 10.dp),
    // 🔽 기본을 소형으로
    avatarSize: Dp = 32.dp,
    nameFontSize: TextUnit = 14.sp,
    metaFontSize: TextUnit = 12.sp,
    horizontalSpacing: Dp = 8.dp,
    verticalPadding: Dp = 10.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),   // ← 내부 좌우 패딩 제거 (부모가 제어)
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (author.isAnonymous) null else author.profileImageUrl,
            contentDescription = "프로필 이미지",
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(horizontalSpacing))

        Column {
            androidx.compose.material3.Text(
                text = if (author.isAnonymous) "익명" else author.nickname,
                fontSize = nameFontSize,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = nameFontSize, // 글자크기와 동일
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Top,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )
            androidx.compose.material3.Text(
                text = TimeFormatter.formatRelativeTime(createdAt),
                fontSize = metaFontSize,
                color = Color(0xFF666666),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = metaFontSize, // 글자크기와 동일
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Top,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )
        }
    }
}
