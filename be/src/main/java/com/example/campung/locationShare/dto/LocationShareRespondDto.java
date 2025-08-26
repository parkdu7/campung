package com.example.campung.locationShare.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareRespondDto {
    
    private String action; // "accept" or "reject"
    private BigDecimal latitude; // 수락할 때만 필요
    private BigDecimal longitude; // 수락할 때만 필요
}