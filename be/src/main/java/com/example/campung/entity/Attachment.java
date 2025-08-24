package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachment",
       indexes = @Index(name = "ix_attachment_content_created", columnList = "content_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;
    
    @Column(name = "original_name", nullable = false)
    private String originalName;
    
    @Column(name = "file_type", nullable = false)
    private String fileType;
    
    @Column(name = "file_size", nullable = false)
    private Integer fileSize;
    
    @Column(nullable = false)
    private String url;
    
    @Column(nullable = false)
    private Integer idx;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}