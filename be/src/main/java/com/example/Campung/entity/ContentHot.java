package com.example.Campung.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_hot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentHot {
    
    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
}