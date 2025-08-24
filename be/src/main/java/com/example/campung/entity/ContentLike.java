package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_like", 
       uniqueConstraints = @UniqueConstraint(name = "uq_content_like_once", 
                                           columnNames = {"content_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_like_id")
    private Long contentLikeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}