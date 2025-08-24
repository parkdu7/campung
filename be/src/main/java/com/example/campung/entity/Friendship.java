package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendship",
       uniqueConstraints = @UniqueConstraint(name = "uq_friendship_pair", 
                                           columnNames = {"requester_id", "addressee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friendship_id")
    private Long friendshipId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;
    
    @Builder.Default
    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'pending'")
    private String status = "pending";
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}