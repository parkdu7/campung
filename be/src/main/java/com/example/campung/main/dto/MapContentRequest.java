package com.example.campung.main.dto;

import com.example.campung.global.enums.PostType;
import io.swagger.v3.oas.annotations.media.Schema;

public class MapContentRequest {
    @Schema(description = "위도", example = "36.0", required = true)
    private Double lat;
    
    @Schema(description = "경도", example = "127.0", required = true)
    private Double lng;
    private Integer radius = 500; // 기본값: 500미터
    private PostType postType;
    private String date;

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public PostType getPostType() {
        return postType;
    }

    public void setPostType(PostType postType) {
        this.postType = postType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}