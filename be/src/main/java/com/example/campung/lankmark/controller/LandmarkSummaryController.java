package com.example.campung.lankmark.controller;

import com.example.campung.lankmark.dto.LandmarkSummaryResponse;
import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import com.example.campung.lankmark.service.GPT5ServiceV3;
import com.example.campung.lankmark.service.LandmarkPostCollectionService;
import com.example.campung.lankmark.service.LandmarkSummaryService;
import com.example.campung.lankmark.service.LandmarkSummaryServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/landmark")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Landmark Summary", description = "랜드마크 요약 생성 API")
public class LandmarkSummaryController {

    private final LandmarkRepository landmarkRepository;
    private final LandmarkSummaryService landmarkSummaryService;
    private final LandmarkSummaryServiceV2 landmarkSummaryServiceV2;
    private final GPT5ServiceV3 gpt5ServiceV3;
    private final LandmarkPostCollectionService postCollectionService;

    @PostMapping("/{landmarkId}/summary")
    @Operation(summary = "랜드마크 요약 생성", 
               description = "GPT-5 Responses API를 사용하여 해당 랜드마크 주변 게시글들을 분석하고 실시간 요약을 생성합니다.")
    public ResponseEntity<LandmarkSummaryResponse> generateLandmarkSummary(
            @Parameter(description = "랜드마크 ID") 
            @PathVariable Long landmarkId,
            
            @Parameter(description = "검색 반경 (미터, 선택사항)") 
            @RequestParam(required = false) Integer radius,
            
            @Parameter(description = "GPT 모델 선택 (gpt-5, gpt-5-mini, gpt-5-nano, 선택사항)") 
            @RequestParam(required = false, defaultValue = "gpt-5") String model,
            
            @Parameter(description = "추론 강도 (low, medium, high, 선택사항)") 
            @RequestParam(required = false, defaultValue = "low") String reasoningEffort,
            
            @Parameter(description = "응답 상세도 (low, medium, high, 선택사항)") 
            @RequestParam(required = false, defaultValue = "medium") String verbosity) {
        
        try {
            // 랜드마크 존재 확인
            Landmark landmark = landmarkRepository.findById(landmarkId)
                    .orElseThrow(() -> new IllegalArgumentException("랜드마크를 찾을 수 없습니다: " + landmarkId));

            // 주변 게시글 수집
            List<LandmarkSummaryService.PostData> posts;
            int effectiveRadius;
            
            if (radius != null && radius > 0) {
                posts = postCollectionService.collectPostsWithRadius(landmark, radius);
                effectiveRadius = radius;
            } else {
                posts = postCollectionService.collectPostsAroundLandmark(landmark);
                effectiveRadius = landmark.getCategory().getDefaultRadius();
            }

            // 공식 GPT-5 가이드를 따라 reasoning_effort 및 verbosity 파라미터 적용
            String summary = gpt5ServiceV3.generateOptimizedSummary(
                    landmarkId, 
                    landmark.getName(), 
                    posts, 
                    effectiveRadius,
                    model,
                    reasoningEffort,
                    verbosity
            );

            // 랜드마크 엔티티에 요약 저장
            landmark.updateSummary(summary);
            landmarkRepository.save(landmark);

            // 키워드 추출 (간단한 로직)
            List<String> keywords = extractKeywords(summary);

            // 응답 생성
            LandmarkSummaryResponse response = LandmarkSummaryResponse.builder()
                    .landmarkId(landmarkId)
                    .summary(summary)
                    .generatedAt(LocalDateTime.now())
                    .postCount(posts.size())
                    .keywords(keywords)
                    .build();

            log.info("랜드마크 {} GPT-5 요약 생성 완료: {}개 게시글 분석 (모델: {}, reasoning_effort: {}, verbosity: {})", 
                    landmark.getName(), posts.size(), model, reasoningEffort, verbosity);
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("랜드마크 요약 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("랜드마크 요약 생성 중 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{landmarkId}/summary/cache")
    @Operation(summary = "랜드마크 요약 캐시 삭제", 
               description = "특정 랜드마크의 요약 캐시를 삭제합니다.")
    public ResponseEntity<Void> clearSummaryCache(
            @Parameter(description = "랜드마크 ID") 
            @PathVariable Long landmarkId) {
        
        try {
            landmarkSummaryService.clearCache(landmarkId);
            landmarkSummaryServiceV2.clearAdvancedCache(landmarkId);
            gpt5ServiceV3.clearAllModelCache(landmarkId);
            log.info("랜드마크 {} 모든 GPT-5 모델 캐시 삭제 완료", landmarkId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("랜드마크 {} 요약 캐시 삭제 실패: {}", landmarkId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 간단한 키워드 추출 로직
     * 추후 더 정교한 NLP 라이브러리로 개선 가능
     */
    private List<String> extractKeywords(String summary) {
        // 따옴표로 둘러싸인 키워드들을 추출
        return Arrays.stream(summary.split("[''\"\"']"))
                .filter(word -> word.length() > 1 && word.length() < 10)
                .filter(word -> !word.matches(".*[.!?].*"))
                .distinct()
                .limit(5)
                .toList();
    }
}