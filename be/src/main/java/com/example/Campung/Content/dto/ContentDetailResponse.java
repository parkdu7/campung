package com.example.campung.content.dto;

public class ContentDetailResponse {
    private boolean success;
    private String message;
    private ContentDetailRequest data;
    
    public ContentDetailResponse(boolean success, String message, ContentDetailRequest data) {
        this.success = success;
        this.message = message;
        this.data = data;
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
    
    public ContentDetailRequest getData() {
        return data;
    }
    
    public void setData(ContentDetailRequest data) {
        this.data = data;
    }
}