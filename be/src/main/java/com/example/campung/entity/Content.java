package com.example.campung.entity;

import com.example.campung.global.enums.PostType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long contentId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType;
    
    @Builder.Default
    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'board'")
    private String status = "board";
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    private String emotion;
    
    @Builder.Default
    @Column(name = "view_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer viewCount = 0;
    
    @Builder.Default
    @Column(name = "like_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer likeCount = 0;
    
    @Builder.Default
    @Column(name = "comment_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer commentCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContentLike> contentLikes;
    
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;
    
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments;
    
    @OneToMany(mappedBy = "reportedContent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reports;
    
    @OneToOne(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ContentHot contentHot;
}