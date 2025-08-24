package com.example.campung.content.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentLikeResponse {
    private boolean success;
    private String message;
    private ContentLikeData data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentLikeData {
        private boolean isLiked;
        private int totalLikes;
    }
}