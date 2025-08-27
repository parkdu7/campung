package com.example.campung.notification.repository;

import com.example.campung.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    long countByUser_UserIdAndIsReadFalse(String userId);
    
    Optional<Notification> findByNotificationIdAndUser_UserId(Long notificationId, String userId);
    
    // JPA 메서드 이름으로 자동 쿼리 생성
    java.util.List<Notification> findByUser_UserIdAndTypeAndDataContaining(String userId, String type, String data);
    
    // 읽지 않은 알림만 조회
    Page<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(String userId, Pageable pageable);
}