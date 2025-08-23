package com.example.Campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "location_share")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_share_id")
    private Long locationShareId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_request_id", nullable = false)
    private LocationRequest locationRequest;
    
    @Column(name = "shared_latitude", precision = 10, scale = 8)
    private BigDecimal sharedLatitude;
    
    @Column(name = "shared_longitude", precision = 11, scale = 8)
    private BigDecimal sharedLongitude;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "display_until")
    private LocalDateTime displayUntil;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}