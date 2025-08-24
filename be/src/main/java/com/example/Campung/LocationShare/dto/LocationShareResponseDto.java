package com.example.Campung.LocationShare.dto;

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