package com.example.campung.notification.dto;

public record NewPostEvent(long postId, double lat, double lon, long createdAt) {
}