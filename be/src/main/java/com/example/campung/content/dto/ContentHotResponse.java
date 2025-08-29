package com.example.campung.content.dto;

import java.util.List;

public class ContentHotResponse {
    private boolean success;
    private String message;
    private List<HotContentItem> data;
    
    public ContentHotResponse() {}
    
    public ContentHotResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ContentHotResponse(boolean success, String message, List<HotContentItem> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<HotContentItem> getData() {
        return data;
    }
    
    public void setData(List<HotContentItem> data) {
        this.data = data;
    }
    
    public static class HotContentItem {
        private Long contentId;
        private String userId;
        private AuthorInfo author;
        private String postType;
        private String title;
        private String content;
        private String createdAt;
        private Long hotScore;
        private Integer likeCount;
        private Integer commentCount;
        private String buildingName;
        private String emotion;
        private String thumbnailUrl;
        private String userProfileUrl;
        
        public HotContentItem() {}
        
        public Long getContentId() {
            return contentId;
        }
        
        public void setContentId(Long contentId) {
            this.contentId = contentId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public AuthorInfo getAuthor() {
            return author;
        }
        
        public void setAuthor(AuthorInfo author) {
            this.author = author;
        }
        
        public String getPostType() {
            return postType;
        }
        
        public void setPostType(String postType) {
            this.postType = postType;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public Long getHotScore() {
            return hotScore;
        }
        
        public void setHotScore(Long hotScore) {
            this.hotScore = hotScore;
        }
        
        public Integer getLikeCount() {
            return likeCount;
        }
        
        public void setLikeCount(Integer likeCount) {
            this.likeCount = likeCount;
        }
        
        public Integer getCommentCount() {
            return commentCount;
        }
        
        public void setCommentCount(Integer commentCount) {
            this.commentCount = commentCount;
        }
        
        public String getBuildingName() {
            return buildingName;
        }
        
        public void setBuildingName(String buildingName) {
            this.buildingName = buildingName;
        }
        
        public String getEmotion() {
            return emotion;
        }
        
        public void setEmotion(String emotion) {
            this.emotion = emotion;
        }
        
        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
        
        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }
        
        public String getUserProfileUrl() {
            return userProfileUrl;
        }
        
        public void setUserProfileUrl(String userProfileUrl) {
            this.userProfileUrl = userProfileUrl;
        }
    }
    
    public static class AuthorInfo {
        private String nickname;
        private Boolean isAnonymous;
        
        public AuthorInfo() {}
        
        public AuthorInfo(String nickname, Boolean isAnonymous) {
            this.nickname = nickname;
            this.isAnonymous = isAnonymous;
        }
        
        public String getNickname() {
            return nickname;
        }
        
        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
        
        public Boolean getIsAnonymous() {
            return isAnonymous;
        }
        
        public void setIsAnonymous(Boolean isAnonymous) {
            this.isAnonymous = isAnonymous;
        }
    }
}