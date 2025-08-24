package com.example.campung.content.dto;

import java.util.List;

public class ContentSearchResponse {
    private boolean success;
    private String message;
    private SearchData data;
    
    public ContentSearchResponse() {}
    
    public ContentSearchResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ContentSearchResponse(boolean success, String message, SearchData data) {
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
    
    public SearchData getData() {
        return data;
    }
    
    public void setData(SearchData data) {
        this.data = data;
    }
    
    public static class SearchData {
        private String query;
        private Integer totalResults;
        private List<ContentItem> contents;
        private PaginationInfo pagination;
        
        public SearchData() {}
        
        public SearchData(String query, Integer totalResults, List<ContentItem> contents, PaginationInfo pagination) {
            this.query = query;
            this.totalResults = totalResults;
            this.contents = contents;
            this.pagination = pagination;
        }
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
        
        public Integer getTotalResults() {
            return totalResults;
        }
        
        public void setTotalResults(Integer totalResults) {
            this.totalResults = totalResults;
        }
        
        public List<ContentItem> getContents() {
            return contents;
        }
        
        public void setContents(List<ContentItem> contents) {
            this.contents = contents;
        }
        
        public PaginationInfo getPagination() {
            return pagination;
        }
        
        public void setPagination(PaginationInfo pagination) {
            this.pagination = pagination;
        }
    }
    
    public static class ContentItem {
        private Long contentId;
        private AuthorInfo author;
        private String postType;
        private String title;
        private String highlight;
        private LocationInfo location;
        private ReactionInfo reactions;
        private String createdAt;
        
        public ContentItem() {}
        
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
        
        public String getHighlight() {
            return highlight;
        }
        
        public void setHighlight(String highlight) {
            this.highlight = highlight;
        }
        
        public LocationInfo getLocation() {
            return location;
        }
        
        public void setLocation(LocationInfo location) {
            this.location = location;
        }
        
        public ReactionInfo getReactions() {
            return reactions;
        }
        
        public void setReactions(ReactionInfo reactions) {
            this.reactions = reactions;
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
    
    public static class LocationInfo {
        private String address;
        
        public LocationInfo() {}
        
        public LocationInfo(String address) {
            this.address = address;
        }
        
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
        }
    }
    
    public static class ReactionInfo {
        private Integer likes;
        private Integer comments;
        
        public ReactionInfo() {}
        
        public ReactionInfo(Integer likes, Integer comments) {
            this.likes = likes;
            this.comments = comments;
        }
        
        public Integer getLikes() {
            return likes;
        }
        
        public void setLikes(Integer likes) {
            this.likes = likes;
        }
        
        public Integer getComments() {
            return comments;
        }
        
        public void setComments(Integer comments) {
            this.comments = comments;
        }
    }
    
    public static class PaginationInfo {
        private Integer currentPage;
        private Integer totalPages;
        private Integer totalElements;
        
        public PaginationInfo() {}
        
        public PaginationInfo(Integer currentPage, Integer totalPages, Integer totalElements) {
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.totalElements = totalElements;
        }
        
        public Integer getCurrentPage() {
            return currentPage;
        }
        
        public void setCurrentPage(Integer currentPage) {
            this.currentPage = currentPage;
        }
        
        public Integer getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }
        
        public Integer getTotalElements() {
            return totalElements;
        }
        
        public void setTotalElements(Integer totalElements) {
            this.totalElements = totalElements;
        }
    }
}