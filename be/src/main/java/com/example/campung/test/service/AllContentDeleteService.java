package com.example.campung.test.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.test.dto.TestContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AllContentDeleteService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Transactional
    public TestContentResponse deleteAllContents() {
        try {
            log.info("=== 모든 컨텐츠 삭제 시작 ===");
            
            // 현재 컨텐츠 개수 확인
            long totalCount = contentRepository.count();
            log.info("삭제 대상 컨텐츠 개수: {}개", totalCount);
            
            if (totalCount == 0) {
                log.info("삭제할 컨텐츠가 없습니다");
                return new TestContentResponse(true, "삭제할 컨텐츠가 없습니다", 0);
            }
            
            // 모든 컨텐츠 삭제 (cascade로 연관 엔티티들 자동 삭제)
            contentRepository.deleteAll();
            log.info("=== 모든 컨텐츠 삭제 완료: {}개 ===", totalCount);
            
            // 삭제 후 확인
            long remainingCount = contentRepository.count();
            if (remainingCount > 0) {
                log.warn("일부 컨텐츠가 삭제되지 않았습니다. 남은 개수: {}", remainingCount);
                return new TestContentResponse(false, 
                    "일부 컨텐츠 삭제에 실패했습니다. 남은 개수: " + remainingCount, 
                    (int)(totalCount - remainingCount));
            }
            
            return new TestContentResponse(true, 
                "모든 컨텐츠가 성공적으로 삭제되었습니다", (int)totalCount);
            
        } catch (Exception e) {
            log.error("모든 컨텐츠 삭제 실패: {}", e.getMessage(), e);
            return new TestContentResponse(false, 
                "컨텐츠 삭제 중 오류가 발생했습니다: " + e.getMessage(), 0);
        }
    }
}