package com.example.campung.content.repository;

import com.example.campung.entity.Content;
import com.example.campung.global.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
           "c.createdAt BETWEEN :startDate AND :endDate AND " +
           "c.isHot = true " +
           "ORDER BY c.createdAt DESC")
    List<Content> findByLocationAndDateAndIsHot(@Param("minLat") double minLat,
                                               @Param("maxLat") double maxLat,
                                               @Param("minLng") double minLng,
                                               @Param("maxLng") double maxLng,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    
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
    
    /**
     * 랜드마크 주변 게시글 조회 (Haversine 공식 사용)
     */
    @Query(value = """
        SELECT c.title, c.content, 
               (6371000 * acos(cos(radians(:latitude)) * cos(radians(CAST(c.latitude AS DOUBLE))) * 
                              cos(radians(CAST(c.longitude AS DOUBLE)) - radians(:longitude)) + 
                              sin(radians(:latitude)) * sin(radians(CAST(c.latitude AS DOUBLE))))) AS distance
        FROM content c
        WHERE c.created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
        HAVING distance <= :radiusMeters
        ORDER BY c.created_at DESC
        """, nativeQuery = true)
    List<Object[]> findNearbyContents(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusMeters") Integer radiusMeters
    );
    
    /**
     * 감정 분석용 시간 범위 게시글 조회
     */
    List<Content> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 시간 범위 내 게시글 수 계산
     */
    int countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 일별 게시글 수 통계 조회
     */
    @Query("SELECT DATE(c.createdAt), COUNT(c) FROM Content c WHERE c.createdAt BETWEEN :startTime AND :endTime GROUP BY DATE(c.createdAt)")
    List<Object[]> findDailyPostCounts(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 가장 오래된 게시글 조회 (서버 시작 시점 파악용)
     */
    Optional<Content> findTopByOrderByCreatedAtAsc();
    
    /**
     * 모든 컨텐츠의 isHot 플래그를 false로 초기화
     */
    @Modifying
    @Query("UPDATE Content c SET c.isHot = false")
    void updateAllIsHotToFalse();
    
    /**
     * 콘텐츠의 좋아요 수 업데이트
     */
    @Modifying
    @Query("UPDATE Content c SET c.likeCount = :likeCount WHERE c.contentId = :contentId")
    void updateLikeCount(@Param("contentId") Long contentId, @Param("likeCount") int likeCount);
}