package com.example.campung.lankmark.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.geo.service.GeohashService;
import com.example.campung.global.enums.LandmarkCategory;
import com.example.campung.lankmark.entity.Landmark;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandmarkPostCollectionService {

    private final ContentRepository contentRepository;
    private final GeohashService geohashService;

    /**
     * 랜드마크 주변의 게시글들을 수집
     */
    public List<LandmarkSummaryService.PostData> collectPostsAroundLandmark(Landmark landmark) {
        // 카테고리별 적응적 반경 설정
        int radius = landmark.getCategory().getDefaultRadius();
        
        log.info("랜드마크 {} 주변 {}m 반경에서 게시글 수집 시작", landmark.getName(), radius);
        
        try {
            // 위치 기반으로 게시글 검색 (Content 엔티티의 latitude, longitude 필드 사용)
            log.info("쿼리 파라미터: 위도={}, 경도={}, 반경={}m", 
                    landmark.getLatitude(), landmark.getLongitude(), radius);
                    
            List<Object[]> nearbyContents = contentRepository.findNearbyContents(
                landmark.getLatitude(),
                landmark.getLongitude(),
                radius
            );
            
            log.info("쿼리 결과: {}개 행 반환", nearbyContents.size());
            
            List<LandmarkSummaryService.PostData> posts = nearbyContents.stream()
                .map(row -> {
                    String title = (String) row[0];
                    String content = (String) row[1]; 
                    log.debug("게시글 발견: 제목='{}', 내용='{}'", title, content);
                    return new LandmarkSummaryService.PostData(title, content);
                })
                .toList();
                
            log.info("랜드마크 {} 주변에서 {}개 게시글 수집 완료", landmark.getName(), posts.size());
            return posts;
            
        } catch (Exception e) {
            log.error("랜드마크 {} 주변 게시글 수집 중 오류 발생: {}", landmark.getName(), e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 특정 반경으로 게시글 수집
     */
    public List<LandmarkSummaryService.PostData> collectPostsWithRadius(
            Landmark landmark, 
            int customRadius) {
        
        log.info("랜드마크 {} 주변 {}m 반경(사용자 지정)에서 게시글 수집", landmark.getName(), customRadius);
        
        try {
            List<Object[]> nearbyContents = contentRepository.findNearbyContents(
                landmark.getLatitude(),
                landmark.getLongitude(),
                customRadius
            );
            
            return nearbyContents.stream()
                .map(row -> new LandmarkSummaryService.PostData(
                    (String) row[0], // title
                    (String) row[1]  // contents
                ))
                .toList();
                
        } catch (Exception e) {
            log.error("랜드마크 {} 주변 게시글 수집 중 오류 발생: {}", landmark.getName(), e.getMessage());
            return List.of();
        }
    }
}