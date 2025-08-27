package com.example.campung.content.exception;

public class ThumbnailGenerationException extends RuntimeException {
    private final String errorCode;
    
    public ThumbnailGenerationException(String message) {
        super(message);
        this.errorCode = "THUMBNAIL_GENERATION_ERROR";
    }
    
    public ThumbnailGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "THUMBNAIL_GENERATION_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}