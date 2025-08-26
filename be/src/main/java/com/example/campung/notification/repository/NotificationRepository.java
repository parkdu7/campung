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
}