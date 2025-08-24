package com.example.Campung.Content.Dto;

import java.util.List;

public class ContentListResponse {
    private boolean success;
    private String message;
    private ListData data;
    
    public ContentListResponse() {}
    
    public ContentListResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ContentListResponse(boolean success, String message, ListData data) {
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
    
    public ListData getData() {
        return data;
    }
    
    public void setData(ListData data) {
        this.data = data;
    }
    
    public static class ListData {
        private String date;
        private List<ContentListItem> contents;
        
        public ListData() {}
        
        public ListData(String date, List<ContentListItem> contents) {
            this.date = date;
            this.contents = contents;
        }
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public List<ContentListItem> getContents() {
            return contents;
        }
        
        public void setContents(List<ContentListItem> contents) {
            this.contents = contents;
        }
    }
    
    public static class ContentListItem {
        private Long contentId;
        private AuthorInfo author;
        private String postType;
        private String title;
        private String createdAt;
        
        public ContentListItem() {}
        
        public Long getContentId() {
            return contentId;
        }
        
        public void setContentId(Long contentId) {
            this.contentId = contentId;
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
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
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