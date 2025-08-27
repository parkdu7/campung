package com.example.campung.locationShare.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareRequestDto {
    
    private List<String> friendUserIds; // Long friendIds -> String friendUserIds로 변경
    private String purpose;
}