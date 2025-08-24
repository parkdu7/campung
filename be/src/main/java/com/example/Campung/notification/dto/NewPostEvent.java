package com.example.Campung.Notification.Dto;

public record NewPostEvent(long postId, double lat, double lon, long createdAt) {
}