package com.example.campung.locationShare.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareResponseDto {
    
    private String message;
    private int successCount;
    private int totalCount;
}