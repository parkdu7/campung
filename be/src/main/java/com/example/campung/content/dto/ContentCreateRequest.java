package com.example.campung.content.dto;

import com.example.campung.global.enums.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ContentCreateRequest {
    @Schema(description = "제목", example = "테스트 게시글")
    private String title;
    
    @Schema(description = "내용", example = "테스트 내용입니다")
    private String body;
    
    @Schema(description = "위도", example = "36.0", required = true)
    private Double latitude;
    
    @Schema(description = "경도", example = "127.0", required = true)
    private Double longitude;
    
    @Schema(description = "콘텐츠 범위", example = "PUBLIC")
    private String contentScope;
    
    @Schema(description = "게시글 타입", example = "INFO")
    private PostType postType;
    
    @Schema(description = "감정 태그", example = "HAPPY")
    private String emotionTag;
    
    @Schema(description = "익명 여부", example = "false")
    private Boolean isAnonymous;
    
    @Schema(description = "첨부 파일들", type = "array", format = "binary")
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