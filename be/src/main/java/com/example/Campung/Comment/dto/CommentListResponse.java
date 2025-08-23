package com.example.Campung.Comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentListResponse {
    private boolean success;
    private String message;
    private CommentData data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentData {
        private List<CommentDto> comments;
    }
}