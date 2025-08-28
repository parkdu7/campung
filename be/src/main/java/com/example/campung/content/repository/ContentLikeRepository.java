package com.example.campung.content.repository;

import com.example.campung.entity.ContentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {
    
    Optional<ContentLike> findByContentContentIdAndUserUserId(Long contentId, String userId);
    
    @Query("SELECT COUNT(cl) FROM ContentLike cl WHERE cl.content.contentId = :contentId")
    int countByContentId(@Param("contentId") Long contentId);
    
    boolean existsByContentContentIdAndUserUserId(Long contentId, String userId);
    
    @Query("SELECT cl FROM ContentLike cl WHERE cl.createdAt >= :since")
    List<ContentLike> findAllSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT cl FROM ContentLike cl WHERE cl.content.contentId = :contentId AND cl.createdAt >= :since")
    List<ContentLike> findByContentIdSince(@Param("contentId") Long contentId, @Param("since") LocalDateTime since);
}