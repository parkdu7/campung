package com.example.campung.test.service;

import com.example.campung.global.enums.EmotionTestType;
import com.example.campung.test.dto.TestContentRequest;
import com.example.campung.test.dto.TestContentResponse;
import com.example.campung.content.dto.ContentCreateRequest;
import com.example.campung.content.service.ContentCreateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TestContentService {
    
    @Autowired
    private ContentCreateService contentCreateService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Pattern countPattern = Pattern.compile("\\{(\\d+)\\}");
    
    @Transactional
    public TestContentResponse createTestContents(TestContentRequest request) {
        try {
            log.info("=== 테스트 컨텐츠 생성 시작 ===");
            log.info("요청 파라미터: lat={}, lng={}, category={}, userId={}, emotionType={}", 
                request.getLatitude(), request.getLongitude(), request.getCategory(), 
                request.getUserId(), request.getEmotionType());
            
            // 선택된 감정 타입에 따른 JSON 파일 읽기
            List<Map<String, String>> testData = loadTestData(request.getEmotionType());
            log.info("테스트 데이터 로드 완료: {}개 (파일: {})", 
                testData.size(), request.getEmotionType().getFileName());
            
            int totalCreated = 0;
            
            // 각 테스트 데이터 처리
            for (Map<String, String> data : testData) {
                String title = data.get("title");
                String content = data.get("content");
                
                // {숫자} 패턴에서 개수 추출
                int count = extractCount(title);
                log.info("제목: '{}', 생성할 개수: {}", title, count);
                
                // {숫자} 부분 제거
                String cleanTitle = title.replaceAll("\\{\\d+\\}", "").trim();
                
                // 지정된 개수만큼 컨텐츠 생성
                for (int i = 0; i < count; i++) {
                    try {
                        ContentCreateRequest contentRequest = createContentRequest(
                            cleanTitle, content, request);
                        contentCreateService.createContent(contentRequest, request.getUserId());
                        totalCreated++;
                        log.debug("컨텐츠 생성 완료: {} ({}/{})", cleanTitle, i + 1, count);
                    } catch (Exception e) {
                        log.error("컨텐츠 생성 실패: {} - {}", cleanTitle, e.getMessage(), e);
                    }
                }
            }
            
            log.info("=== 테스트 컨텐츠 생성 완료: 총 {}개 ===", totalCreated);
            return new TestContentResponse(true, 
                "테스트 컨텐츠가 성공적으로 생성되었습니다", totalCreated);
            
        } catch (Exception e) {
            log.error("테스트 컨텐츠 생성 실패: {}", e.getMessage(), e);
            return new TestContentResponse(false, 
                "테스트 컨텐츠 생성 중 오류가 발생했습니다: " + e.getMessage(), 0);
        }
    }
    
    private List<Map<String, String>> loadTestData(EmotionTestType emotionType) throws IOException {
        String fileName = emotionType.getFileName();
        log.info("로드할 파일: {}", fileName);
        
        ClassPathResource resource = new ClassPathResource(fileName);
        if (!resource.exists()) {
            log.error("파일을 찾을 수 없습니다: {}", fileName);
            throw new IOException("테스트 데이터 파일을 찾을 수 없습니다: " + fileName);
        }
        
        return objectMapper.readValue(resource.getInputStream(), 
            new TypeReference<List<Map<String, String>>>() {});
    }
    
    private int extractCount(String title) {
        Matcher matcher = countPattern.matcher(title);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1; // 기본값
    }
    
    private ContentCreateRequest createContentRequest(String title, String content, 
                                                    TestContentRequest request) {
        ContentCreateRequest contentRequest = new ContentCreateRequest();
        contentRequest.setTitle(title);
        contentRequest.setBody(content);
        contentRequest.setLatitude(request.getLatitude());
        contentRequest.setLongitude(request.getLongitude());
        contentRequest.setPostType(request.getCategory());
        contentRequest.setIsAnonymous(true); // 고정값
        // emotion은 설정하지 않음 (GPT가 분석하도록)
        return contentRequest;
    }
}