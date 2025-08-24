package com.example.campung.content.repository;

import com.example.campung.entity.Content;
import com.example.campung.global.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    @Query("SELECT c FROM Content c WHERE " +
           "(:query IS NULL OR c.title LIKE %:query% OR c.content LIKE %:query%) AND " +
           "(:postType IS NULL OR c.postType = :postType)")
    Page<Content> searchContents(@Param("query") String query, 
                               @Param("postType") PostType postType, 
                               Pageable pageable);
    
    @Query("SELECT c FROM Content c WHERE " +
           "(:postType IS NULL OR c.postType = :postType) " +
           "ORDER BY c.createdAt DESC")
    List<Content> findContentsByPostType(@Param("postType") PostType postType);
    
    @Query("SELECT c FROM Content c WHERE " +
           "c.latitude BETWEEN :minLat AND :maxLat AND " +
           "c.longitude BETWEEN :minLng AND :maxLng AND " +
           "c.createdAt BETWEEN :startDate AND :endDate AND " +
           "c.postType = :postType " +
           "ORDER BY c.createdAt DESC")
    List<Content> findByLocationAndDateAndPostType(@Param("minLat") double minLat,
                                                  @Param("maxLat") double maxLat,
                                                  @Param("minLng") double minLng,
                                                  @Param("maxLng") double maxLng,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  @Param("postType") PostType postType);
    
    @Query("SELECT c FROM Content c WHERE " +
           "c.latitude BETWEEN :minLat AND :maxLat AND " +
           "c.longitude BETWEEN :minLng AND :maxLng AND " +
           "c.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.createdAt DESC")
    List<Content> findByLocationAndDate(@Param("minLat") double minLat,
                                       @Param("maxLat") double maxLat,
                                       @Param("minLng") double minLng,
                                       @Param("maxLng") double maxLng,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}