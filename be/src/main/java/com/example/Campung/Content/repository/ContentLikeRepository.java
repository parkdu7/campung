package com.example.Campung.Content.repository;

import com.example.Campung.entity.ContentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {
    
    Optional<ContentLike> findByContentContentIdAndUserUserId(Long contentId, String userId);
    
    @Query("SELECT COUNT(cl) FROM ContentLike cl WHERE cl.content.contentId = :contentId")
    int countByContentId(@Param("contentId") Long contentId);
    
    boolean existsByContentContentIdAndUserUserId(Long contentId, String userId);
}