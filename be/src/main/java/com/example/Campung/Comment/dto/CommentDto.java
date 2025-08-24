package com.example.Campung.Comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long commentId;
    private String userId;
    private AuthorDto author;
    private String body;
    private List<MediaFileDto> mediaFiles;
    private LocalDateTime createdAt;
    private List<ReplyDto> replies;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private String nickname;
        private String profileImageUrl;
        private boolean isAnonymous;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaFileDto {
        private Long fileId;
        private String fileType;
        private String fileUrl;
        private String thumbnailUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyDto {
        private Long replyId;
        private String userId;
        private AuthorDto author;
        private String body;
        private List<MediaFileDto> mediaFiles;
        private LocalDateTime createdAt;
    }
}