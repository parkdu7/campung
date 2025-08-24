package com.example.campung.locationShare.repository;

import com.example.campung.entity.LocationRequest;
import com.example.campung.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRequestRepository extends JpaRepository<LocationRequest, Long> {
    
    List<LocationRequest> findByToUserAndStatusOrderByCreatedAtDesc(User toUser, String status);
    
    List<LocationRequest> findByFromUserOrderByCreatedAtDesc(User fromUser);
    
    @Query("SELECT lr FROM LocationRequest lr WHERE lr.toUser.userId = :userId AND lr.status = 'pending' AND lr.expiresAt > :now")
    List<LocationRequest> findActiveRequestsByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    Optional<LocationRequest> findByLocationRequestIdAndToUser(Long locationRequestId, User toUser);
    
    @Query("SELECT lr FROM LocationRequest lr WHERE lr.expiresAt < :now AND lr.status = 'pending'")
    List<LocationRequest> findExpiredRequests(@Param("now") LocalDateTime now);
}