package com.example.campung.lankmark.entity;

import com.example.campung.global.enums.LandmarkCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "landmarks")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Landmark {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LandmarkCategory category;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Column(length = 500)
    private String thumbnailUrl;
    
    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;
    
    @Builder.Default
    @Column(nullable = false)
    private Long likeCount = 0L;
    
    @Column(columnDefinition = "TEXT")
    private String currentSummary;
    
    private LocalDateTime summaryUpdatedAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateSummary(String summary) {
        this.currentSummary = summary;
        this.summaryUpdatedAt = LocalDateTime.now();
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}