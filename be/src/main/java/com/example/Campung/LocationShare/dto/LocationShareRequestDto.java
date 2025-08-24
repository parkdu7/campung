package com.example.Campung.LocationShare.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareRequestDto {
    
    private List<Long> friendIds;
    private String purpose;
}