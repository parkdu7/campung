package com.example.campung.record.dto;

import org.springframework.web.multipart.MultipartFile;

public class RecordCreateRequest {
    private MultipartFile audioFile;
    private Double latitude;
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