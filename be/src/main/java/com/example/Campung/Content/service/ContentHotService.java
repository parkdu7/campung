package com.example.campung.content.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.entity.Content;
import com.example.campung.entity.ContentHot;
import com.example.campung.content.repository.ContentHotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContentHotService {
    
    @Autowired
    private ContentHotTrackingService contentHotTrackingService;
    
    @Autowired
    private ContentHotRepository contentHotRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Transactional
    public void updateHotContent() {
        // Redis에서 상위 10개 HOT 컨텐츠 가져오기
        Set<Object> hotContentIds = contentHotTrackingService.getTop10HotContent();
        
        // 기존 HOT 컨텐츠 모두 삭제
        contentHotRepository.deleteAll();
        
        // 새로운 HOT 컨텐츠 저장
        for (Object contentIdObj : hotContentIds) {
            Long contentId = Long.valueOf(contentIdObj.toString());
            long hotScore = contentHotTrackingService.getLike24hCount(contentId);
            
            // 최소 좋아요 수 조건 (예: 5개 이상)
            if (hotScore >= 5) {
                contentRepository.findById(contentId).ifPresent(content -> {
                    ContentHot contentHot = ContentHot.builder()
                            .contentId(contentId)
                            .content(content)
                            .hotScore(hotScore)
                            .build();
                    contentHotRepository.save(contentHot);
                });
            }
        }
    }
    
    public List<Content> getHotContent() {
        List<ContentHot> hotContents = contentHotRepository.findTop10ByOrderByHotScoreDesc();
        return hotContents.stream()
                .map(ContentHot::getContent)
                .collect(Collectors.toList());
    }
    
    public boolean isHotContent(Long contentId) {
        return contentHotRepository.existsByContentId(contentId);
    }
}