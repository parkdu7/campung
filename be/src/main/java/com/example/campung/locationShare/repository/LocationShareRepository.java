package com.example.campung.locationShare.repository;

import com.example.campung.entity.LocationShare;
import com.example.campung.entity.LocationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LocationShareRepository extends JpaRepository<LocationShare, Long> {
    
    List<LocationShare> findByLocationRequestOrderByCreatedAtDesc(LocationRequest locationRequest);
    
    @Query("SELECT ls FROM LocationShare ls WHERE ls.locationRequest.fromUser.userId = :userId AND ls.displayUntil > :now")
    List<LocationShare> findActiveSharesByRequesterId(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT ls FROM LocationShare ls WHERE ls.displayUntil < :now")
    List<LocationShare> findExpiredShares(@Param("now") LocalDateTime now);
}