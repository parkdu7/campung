package com.example.campung.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsRequest {
    
    private Boolean likes;
    private Boolean comments;
    private Boolean location;
}