package com.example.Campung.notification.repository;

import com.example.Campung.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") String userId);
    
    @Query("SELECT n FROM Notification n WHERE n.notificationId = :notificationId AND n.user.userId = :userId")
    Optional<Notification> findByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") String userId);
}