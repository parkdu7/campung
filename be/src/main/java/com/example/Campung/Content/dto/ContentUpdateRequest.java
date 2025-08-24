package com.example.Campung.Content.Dto;

import com.example.Campung.Global.Enum.PostType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ContentUpdateRequest {
    private String title;
    private String body;
    private Double latitude;
    private Double longitude;
    private String contentScope;
    private PostType postType;
    private String emotionTag;
    private Boolean isAnonymous;
    private List<MultipartFile> newFiles; // 새로 추가할 파일들
    private List<Long> deleteFileIds; // 삭제할 파일 ID 목록
    
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
    
    public List<MultipartFile> getNewFiles() {
        return newFiles;
    }
    
    public void setNewFiles(List<MultipartFile> newFiles) {
        this.newFiles = newFiles;
    }
    
    public List<Long> getDeleteFileIds() {
        return deleteFileIds;
    }
    
    public void setDeleteFileIds(List<Long> deleteFileIds) {
        this.deleteFileIds = deleteFileIds;
    }
}