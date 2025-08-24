package com.example.campung.notification.repository;

import com.example.campung.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    
    @Query("SELECT ns FROM NotificationSetting ns WHERE ns.user.userId = :userId")
    Optional<NotificationSetting> findByUserId(@Param("userId") String userId);
}