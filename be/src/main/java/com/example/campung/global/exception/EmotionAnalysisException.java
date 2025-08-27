package com.example.campung.global.exception;

/**
 * 감정 분석 처리 중 발생하는 예외
 */
public class EmotionAnalysisException extends RuntimeException {
    public EmotionAnalysisException(String message) {
        super(message);
    }
    
    public EmotionAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}