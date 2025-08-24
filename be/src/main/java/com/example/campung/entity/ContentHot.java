package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_hot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentHot {
    
    @Id
    @Column(name = "content_id")
    private Long contentId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false)
    private Content content;
    
    @Builder.Default
    @Column(name = "hot_score", columnDefinition = "BIGINT DEFAULT 0")
    private Long hotScore = 0L;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}