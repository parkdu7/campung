package com.example.Campung.Content.dto;

public class ContentDeleteResponse {
    private boolean success;
    private String message;
    private Long contentId;
    
    public ContentDeleteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ContentDeleteResponse(boolean success, String message, Long contentId) {
        this.success = success;
        this.message = message;
        this.contentId = contentId;
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
    
    public Long getContentId() {
        return contentId;
    }
    
    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }
}