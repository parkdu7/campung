package com.example.Campung.LocationShare.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareRespondResponseDto {
    
    private String message;
    private String status; // "accepted" or "rejected"
}