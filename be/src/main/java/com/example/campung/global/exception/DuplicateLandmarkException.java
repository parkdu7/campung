package com.example.campung.global.exception;

public class DuplicateLandmarkException extends RuntimeException {
    public DuplicateLandmarkException(String message) {
        super(message);
    }
    
    public static DuplicateLandmarkException forLandmark(String landmarkName) {
        return new DuplicateLandmarkException(String.format("같은 위치에 '%s' 랜드마크가 이미 존재합니다.", landmarkName));
    }
}