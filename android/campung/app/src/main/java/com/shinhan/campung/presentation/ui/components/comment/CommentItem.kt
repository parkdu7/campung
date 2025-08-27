package com.shinhan.campung.presentation.ui.components.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shinhan.campung.data.model.Comment
import com.shinhan.campung.presentation.ui.theme.CampusSecondary
import com.shinhan.campung.presentation.utils.TimeFormatter

@Composable
fun CommentItem(
    comment: Comment,
    onReplyClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) CampusSecondary.copy(alpha = 0.1f) else Color.Transparent
            )
    ) {
        // 댓글
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AsyncImage(
                model = if (comment.author.isAnonymous) null else comment.author.profileImageUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (comment.author.isAnonymous) "익명" else comment.author.nickname,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TimeFormatter.formatRelativeTime(comment.createdAtDateTime),
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = comment.body,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "답글 달기",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.clickable { onReplyClick(comment.commentId) }
                )
            }
        }

        // 대댓글
        comment.replies?.forEach { reply ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            ) {
                AsyncImage(
                    model = if (reply.author.isAnonymous) null else reply.author.profileImageUrl,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (reply.author.isAnonymous) "익명" else reply.author.nickname,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TimeFormatter.formatRelativeTime(reply.createdAtDateTime),
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = reply.body,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}