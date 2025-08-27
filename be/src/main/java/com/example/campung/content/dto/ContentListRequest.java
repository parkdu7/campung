package com.example.campung.content.dto;

import com.example.campung.global.enums.PostType;
import io.swagger.v3.oas.annotations.media.Schema;

public class ContentListRequest {
    @Schema(description = "조회할 날짜", example = "2024-01-01", required = true)
    private String date;
    
    @Schema(description = "위도", example = "36.0")
    private Double lat;
    
    @Schema(description = "경도", example = "127.0")
    private Double lng;
    private Integer radius;
    private PostType postType;
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
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
}