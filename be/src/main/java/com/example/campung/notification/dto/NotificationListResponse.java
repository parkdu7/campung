package com.example.campung.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponse {
    
    private List<NotificationResponse> notifications;
    private long unreadCount;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int size;
}