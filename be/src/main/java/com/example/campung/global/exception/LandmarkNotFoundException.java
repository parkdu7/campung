package com.example.campung.global.exception;

public class LandmarkNotFoundException extends RuntimeException {
    public LandmarkNotFoundException(String message) {
        super(message);
    }
    
    public LandmarkNotFoundException(Long landmarkId) {
        super("랜드마크를 찾을 수 없습니다: " + landmarkId);
    }
}