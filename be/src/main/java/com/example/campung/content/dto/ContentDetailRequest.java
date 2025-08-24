package com.example.campung.content.dto;

import java.util.List;

public class ContentDetailRequest {
    private Long contentId;
    private String userId;
    private AuthorInfo author;
    private LocationInfo location;
    private String postType;
    private String title;
    private String body;
    private List<MediaFileInfo> mediaFiles;
    private boolean isHotContent;
    
    public static class AuthorInfo {
        private String nickname;
        private String profileImageUrl;
        private Boolean isAnonymous;
        
        public AuthorInfo(String nickname, String profileImageUrl, Boolean isAnonymous) {
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
            this.isAnonymous = isAnonymous;
        }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
        public Boolean getIsAnonymous() { return isAnonymous; }
        public void setIsAnonymous(Boolean isAnonymous) { this.isAnonymous = isAnonymous; }
    }
    
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
        private String address;
        private String buildingName;
        
        public LocationInfo(Double latitude, Double longitude, String address, String buildingName) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.buildingName = buildingName;
        }
        
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    }
    
    public static class MediaFileInfo {
        private Long fileId;
        private String fileType;
        private String fileUrl;
        private String thumbnailUrl;
        private String fileName;
        private Integer fileSize;
        private Integer order;
        
        public MediaFileInfo(Long fileId, String fileType, String fileUrl, String thumbnailUrl, 
                           String fileName, Integer fileSize, Integer order) {
            this.fileId = fileId;
            this.fileType = fileType;
            this.fileUrl = fileUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.order = order;
        }
        
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Integer getFileSize() { return fileSize; }
        public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
    }
    
    // Main class getters and setters
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public AuthorInfo getAuthor() { return author; }
    public void setAuthor(AuthorInfo author) { this.author = author; }
    public LocationInfo getLocation() { return location; }
    public void setLocation(LocationInfo location) { this.location = location; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public List<MediaFileInfo> getMediaFiles() { return mediaFiles; }
    public void setMediaFiles(List<MediaFileInfo> mediaFiles) { this.mediaFiles = mediaFiles; }
    public boolean isHotContent() { return isHotContent; }
    public void setHotContent(boolean hotContent) { isHotContent = hotContent; }
}