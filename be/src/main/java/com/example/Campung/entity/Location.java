package com.example.Campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long locationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(precision = 10, scale = 8, nullable = false)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8, nullable = false)
    private BigDecimal longitude;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal accuracy;
    
    @Column(name = "is_at_target", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isAtTarget = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}