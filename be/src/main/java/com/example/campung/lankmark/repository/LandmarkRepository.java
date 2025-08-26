package com.example.campung.lankmark.repository;

import com.example.campung.lankmark.entity.Landmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandmarkRepository extends JpaRepository<Landmark, Long> {
    
    /**
     * 특정 위치에서 반경 내의 랜드마크들을 거리순으로 조회
     * Haversine 공식을 사용하여 거리 계산
     */
    @Query(value = """
        SELECT *, 
               (6371000 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * 
                              cos(radians(longitude) - radians(:longitude)) + 
                              sin(radians(:latitude)) * sin(radians(latitude)))) AS distance
        FROM landmarks
        HAVING distance <= :radiusMeters
        ORDER BY distance
        """, nativeQuery = true)
    List<Landmark> findNearbyLandmarks(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusMeters") Integer radiusMeters
    );
    
    /**
     * 특정 위치에서 가장 가까운 랜드마크 찾기
     */
    @Query(value = """
        SELECT *, 
               (6371000 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * 
                              cos(radians(longitude) - radians(:longitude)) + 
                              sin(radians(:latitude)) * sin(radians(latitude)))) AS distance
        FROM landmarks
        ORDER BY distance
        LIMIT 1
        """, nativeQuery = true)
    Landmark findNearestLandmark(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude
    );
    
    /**
     * 이름으로 랜드마크 검색 (부분일치)
     */
    List<Landmark> findByNameContainingIgnoreCase(String name);
    
    /**
     * 카테고리별 랜드마크 조회
     */
    List<Landmark> findByCategoryOrderByCreatedAtDesc(String category);
    
    /**
     * 요약이 있는 랜드마크만 조회
     */
    @Query("SELECT l FROM Landmark l WHERE l.currentSummary IS NOT NULL AND l.currentSummary != ''")
    List<Landmark> findLandmarksWithSummary();
}