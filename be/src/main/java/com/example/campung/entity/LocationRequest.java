package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "location_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_request_id")
    private Long locationRequestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Builder.Default
    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'pending'")
    private String status = "pending";
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "locationRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationShare> locationShares;
}