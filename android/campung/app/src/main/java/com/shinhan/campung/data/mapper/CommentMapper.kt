package com.shinhan.campung.data.mapper

import com.shinhan.campung.data.model.Author
import com.shinhan.campung.data.model.Comment
import com.shinhan.campung.data.model.MediaFile
import com.shinhan.campung.data.model.Reply
import com.shinhan.campung.data.remote.dto.AuthorDto
import com.shinhan.campung.data.remote.dto.CommentDto
import com.shinhan.campung.data.remote.dto.MediaFileDto
import com.shinhan.campung.data.remote.dto.ReplyDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentMapper @Inject constructor() {

    fun toDomain(dto: CommentDto): Comment {
        return Comment(
            commentId = dto.commentId,
            userId = dto.userId,
            author = toDomain(dto.author),
            body = dto.body,
            mediaFiles = dto.mediaFiles?.map { toDomain(it) },
            createdAt = dto.createdAt,
            replies = dto.replies?.map { toDomain(it) },
            replyCount = dto.replyCount
        )
    }

    fun toDomain(dto: ReplyDto): Reply {
        return Reply(
            replyId = dto.replyId,
            userId = dto.userId,
            author = toDomain(dto.author),
            body = dto.body,
            mediaFiles = dto.mediaFiles?.map { toDomain(it) },
            createdAt = dto.createdAt
        )
    }

    fun toDomain(dto: AuthorDto): Author {
        return Author(
            nickname = dto.nickname,
            profileImageUrl = dto.profileImageUrl,
            isAnonymous = dto.isAnonymous
        )
    }

    fun toDomain(dto: MediaFileDto): MediaFile {
        return MediaFile(
            fileId = dto.fileId,
            fileType = dto.fileType,
            fileUrl = dto.fileUrl,
            thumbnailUrl = dto.thumbnailUrl,
            fileName = null,
            fileSize = null,
            order = null
        )
    }
}