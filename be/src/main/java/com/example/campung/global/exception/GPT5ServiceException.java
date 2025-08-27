package com.example.campung.global.exception;

/**
 * GPT-5 서비스 호출 중 발생하는 예외
 */
public class GPT5ServiceException extends RuntimeException {
    public GPT5ServiceException(String message) {
        super(message);
    }
    
    public GPT5ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}