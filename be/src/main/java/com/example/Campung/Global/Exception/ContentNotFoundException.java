package com.example.Campung.Global.Exception;

public class ContentNotFoundException extends RuntimeException {
    public ContentNotFoundException(String message) {
        super(message);
    }
    
    public ContentNotFoundException(Long contentId) {
        super("존재하지 않는 게시글입니다. ID: " + contentId);
    }
}