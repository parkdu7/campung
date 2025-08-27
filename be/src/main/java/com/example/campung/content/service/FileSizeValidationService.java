package com.example.campung.content.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileSizeValidationService {
    
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_AUDIO_SIZE = 10 * 1024 * 1024; // 10MB
    
    public void validateFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        String fileName = file.getOriginalFilename();
        
        log.info("파일 크기 검증 시작: {} ({}bytes, {})", fileName, fileSize, contentType);
        
        if (contentType == null) {
            log.warn("파일 타입을 확인할 수 없음: {}", fileName);
            throw new IllegalArgumentException("파일 타입을 확인할 수 없습니다: " + fileName);
        }
        
        if (contentType.startsWith("image/")) {
            if (fileSize > MAX_IMAGE_SIZE) {
                log.warn("이미지 파일 크기 초과: {} ({}bytes > {}bytes)", fileName, fileSize, MAX_IMAGE_SIZE);
                throw new IllegalArgumentException("이미지 파일은 5MB를 초과할 수 없습니다: " + fileName);
            }
        } else if (contentType.startsWith("video/")) {
            if (fileSize > MAX_VIDEO_SIZE) {
                log.warn("영상 파일 크기 초과: {} ({}bytes > {}bytes)", fileName, fileSize, MAX_VIDEO_SIZE);
                throw new IllegalArgumentException("영상 파일은 100MB를 초과할 수 없습니다: " + fileName);
            }
        } else if (contentType.startsWith("audio/")) {
            if (fileSize > MAX_AUDIO_SIZE) {
                log.warn("음성 파일 크기 초과: {} ({}bytes > {}bytes)", fileName, fileSize, MAX_AUDIO_SIZE);
                throw new IllegalArgumentException("음성 파일은 10MB를 초과할 수 없습니다: " + fileName);
            }
        }
        
        log.info("파일 크기 검증 완료: {}", fileName);
    }
    
    public void validateAudioFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("음성 파일이 비어있음");
            throw new IllegalArgumentException("음성 파일이 필요합니다");
        }
        
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();
        
        log.info("음성 파일 검증 시작: {} ({}bytes, {})", fileName, fileSize, contentType);
        
        if (contentType == null || !contentType.startsWith("audio/")) {
            log.warn("올바르지 않은 음성 파일 타입: {} ({})", fileName, contentType);
            throw new IllegalArgumentException("오디오 파일만 업로드 가능합니다");
        }
        
        if (fileSize > MAX_AUDIO_SIZE) {
            log.warn("음성 파일 크기 초과: {} ({}bytes > {}bytes)", fileName, fileSize, MAX_AUDIO_SIZE);
            throw new IllegalArgumentException("음성 파일은 10MB를 초과할 수 없습니다");
        }
        
        log.info("음성 파일 검증 완료: {}", fileName);
    }
}