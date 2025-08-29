package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private String nickname;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // ★ 추가: FCM 토큰
    @Column(name = "fcm_token")
    private String fcmToken;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Location> locations;
    
    @OneToMany(mappedBy = "fromUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationRequest> sentLocationRequests;
    
    @OneToMany(mappedBy = "toUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationRequest> receivedLocationRequests;
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Content> contents;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContentLike> contentLikes;
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NotificationSetting notificationSetting;
    
    @OneToMany(mappedBy = "requester", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Friendship> sentFriendshipRequests;
    
    @OneToMany(mappedBy = "addressee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Friendship> receivedFriendshipRequests;
    
    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reportedBy;
    
    @OneToMany(mappedBy = "reportedUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reportedUser;
    
    @OneToMany(mappedBy = "reviewedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reviewedReports;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Record> records;
}