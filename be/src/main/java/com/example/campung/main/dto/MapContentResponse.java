package com.example.campung.main.dto;

import java.util.List;

public class MapContentResponse {
    private boolean success;
    private String message;
    private MapContentData data;

    public MapContentResponse() {}

    public MapContentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public MapContentResponse(boolean success, String message, MapContentData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static class MapContentData {
        private List<MapContentItem> contents;
        private List<RecordItem> records;
        private int totalCount;
        private boolean hasMore;
        private String emotionWeather;
        private Double emotionTemperature;
        private Double maxTemperature;
        private Double minTemperature;

        public MapContentData(List<MapContentItem> contents) {
            this.contents = contents;
            this.totalCount = contents.size();
            this.hasMore = false; // 일단 기본값
        }

        public MapContentData(List<MapContentItem> contents, List<RecordItem> records) {
            this.contents = contents;
            this.records = records;
            this.totalCount = contents.size() + (records != null ? records.size() : 0);
            this.hasMore = false;
        }

        public MapContentData(List<MapContentItem> contents, List<RecordItem> records, int totalCount, boolean hasMore) {
            this.contents = contents;
            this.records = records;
            this.totalCount = totalCount;
            this.hasMore = hasMore;
        }

        public List<MapContentItem> getContents() {
            return contents;
        }

        public void setContents(List<MapContentItem> contents) {
            this.contents = contents;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public boolean isHasMore() {
            return hasMore;
        }

        public void setHasMore(boolean hasMore) {
            this.hasMore = hasMore;
        }

        public List<RecordItem> getRecords() {
            return records;
        }

        public void setRecords(List<RecordItem> records) {
            this.records = records;
        }

        public String getEmotionWeather() {
            return emotionWeather;
        }

        public void setEmotionWeather(String emotionWeather) {
            this.emotionWeather = emotionWeather;
        }

        public Double getEmotionTemperature() {
            return emotionTemperature;
        }

        public void setEmotionTemperature(Double emotionTemperature) {
            this.emotionTemperature = emotionTemperature;
        }

        public Double getMaxTemperature() {
            return maxTemperature;
        }

        public void setMaxTemperature(Double maxTemperature) {
            this.maxTemperature = maxTemperature;
        }

        public Double getMinTemperature() {
            return minTemperature;
        }

        public void setMinTemperature(Double minTemperature) {
            this.minTemperature = minTemperature;
        }
    }

    public static class MapContentItem {
        private Long contentId;
        private String userId;
        private AuthorInfo author;
        private LocationInfo location;
        private String postType;
        private String postTypeName;
        private String markerType;
        private String contentScope;
        private String contentType;
        private String title;
        private String body;
        private List<MediaFileInfo> mediaFiles;
        private String emotionTag;
        private ReactionInfo reactions;
        private String createdAt;
        private String expiresAt;

        public MapContentItem() {}

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

        public LocationInfo getLocation() {
            return location;
        }

        public void setLocation(LocationInfo location) {
            this.location = location;
        }

        public String getPostType() {
            return postType;
        }

        public void setPostType(String postType) {
            this.postType = postType;
        }

        public String getPostTypeName() {
            return postTypeName;
        }

        public void setPostTypeName(String postTypeName) {
            this.postTypeName = postTypeName;
        }

        public String getMarkerType() {
            return markerType;
        }

        public void setMarkerType(String markerType) {
            this.markerType = markerType;
        }

        public String getContentScope() {
            return contentScope;
        }

        public void setContentScope(String contentScope) {
            this.contentScope = contentScope;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

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

        public List<MediaFileInfo> getMediaFiles() {
            return mediaFiles;
        }

        public void setMediaFiles(List<MediaFileInfo> mediaFiles) {
            this.mediaFiles = mediaFiles;
        }

        public String getEmotionTag() {
            return emotionTag;
        }

        public void setEmotionTag(String emotionTag) {
            this.emotionTag = emotionTag;
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

        public String getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(String expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    public static class MediaFileInfo {
        private String thumbnailUrl;

        public MediaFileInfo() {}

        public MediaFileInfo(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    public static class ReactionInfo {
        private int likes;
        private int comments;

        public ReactionInfo() {}

        public ReactionInfo(int likes, int comments) {
            this.likes = likes;
            this.comments = comments;
        }

        public int getLikes() {
            return likes;
        }

        public void setLikes(int likes) {
            this.likes = likes;
        }

        public int getComments() {
            return comments;
        }

        public void setComments(int comments) {
            this.comments = comments;
        }
    }

    public static class RecordItem {
        private Long recordId;
        private String userId;
        private AuthorInfo author;
        private LocationInfo location;
        private String recordUrl;
        private String createdAt;

        public RecordItem() {}

        public Long getRecordId() {
            return recordId;
        }

        public void setRecordId(Long recordId) {
            this.recordId = recordId;
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

        public LocationInfo getLocation() {
            return location;
        }

        public void setLocation(LocationInfo location) {
            this.location = location;
        }

        public String getRecordUrl() {
            return recordUrl;
        }

        public void setRecordUrl(String recordUrl) {
            this.recordUrl = recordUrl;
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
        private boolean isAnonymous;

        public AuthorInfo() {}

        public AuthorInfo(String nickname, boolean isAnonymous) {
            this.nickname = nickname;
            this.isAnonymous = isAnonymous;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public boolean isAnonymous() {
            return isAnonymous;
        }

        public void setAnonymous(boolean anonymous) {
            isAnonymous = anonymous;
        }
    }

    public static class LocationInfo {
        private double latitude;
        private double longitude;

        public LocationInfo() {}

        public LocationInfo(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
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

    public MapContentData getData() {
        return data;
    }

    public void setData(MapContentData data) {
        this.data = data;
    }
}