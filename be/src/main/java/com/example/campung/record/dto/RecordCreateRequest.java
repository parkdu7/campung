package com.example.campung.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public class RecordCreateRequest {
    @Schema(description = "오디오 파일", required = true)
    private MultipartFile audioFile;
    
    @Schema(description = "위도", example = "36.0", required = true)
    private Double latitude;
    
    @Schema(description = "경도", example = "127.0", required = true)
    private Double longitude;

    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}