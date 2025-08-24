package com.example.campung.comment.dto;

public class CommentCreateResponse {
    private boolean success;
    private String message;
    private Long commentId;
    
    public CommentCreateResponse() {}
    
    public CommentCreateResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public CommentCreateResponse(boolean success, String message, Long commentId) {
        this.success = success;
        this.message = message;
        this.commentId = commentId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getCommentId() {
        return commentId;
    }
    
    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }
}