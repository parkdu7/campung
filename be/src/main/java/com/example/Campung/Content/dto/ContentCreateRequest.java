package com.example.Campung.Content.dto;

import com.example.Campung.Global.Enum.PostType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ContentCreateRequest {
    private String title;
    private String body;
    private Double latitude;
    private Double longitude;
    private String contentScope;
    private PostType postType;
    private String emotionTag;
    private Boolean isAnonymous;
    private List<MultipartFile> files;
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
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
    
    public String getContentScope() {
        return contentScope;
    }
    
    public void setContentScope(String contentScope) {
        this.contentScope = contentScope;
    }
    
    public PostType getPostType() {
        return postType;
    }
    
    public void setPostType(PostType postType) {
        this.postType = postType;
    }
    
    public String getEmotionTag() {
        return emotionTag;
    }
    
    public void setEmotionTag(String emotionTag) {
        this.emotionTag = emotionTag;
    }
    
    public Boolean getIsAnonymous() {
        return isAnonymous;
    }
    
    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }
    
    public List<MultipartFile> getFiles() {
        return files;
    }
    
    public void setFiles(List<MultipartFile> files) {
        this.files = files;
    }
}