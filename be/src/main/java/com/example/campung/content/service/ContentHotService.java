package com.example.campung.content.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.content.dto.ContentHotResponse;
import com.example.campung.entity.Content;
import com.example.campung.entity.ContentHot;
import com.example.campung.content.repository.ContentHotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
        // 기존 좋아요 데이터를 Redis로 마이그레이션
        contentHotTrackingService.migrateExistingLikesToRedis();
        
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
    
    public ContentHotResponse getHotContents() {
        List<ContentHot> hotContents = contentHotRepository.findTop10ByOrderByHotScoreDesc();
        
        if (hotContents.isEmpty()) {
            return new ContentHotResponse(true, "현재 인기 게시글이 없습니다.");
        }
        
        List<ContentHotResponse.HotContentItem> hotContentItems = hotContents.stream()
                .map(contentHot -> {
                    Content content = contentHot.getContent();
                    ContentHotResponse.HotContentItem item = new ContentHotResponse.HotContentItem();
                    
                    item.setContentId(content.getContentId());
                    item.setTitle(content.getTitle());
                    item.setContent(content.getContent());
                    item.setPostType(content.getPostType().name());
                    item.setCreatedAt(content.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    item.setHotScore(contentHot.getHotScore());
                    item.setLikeCount(content.getLikeCount());
                    item.setCommentCount(content.getCommentCount());
                    item.setBuildingName(content.getBuildingName());
                    item.setEmotion(content.getEmotion());
                    
                    ContentHotResponse.AuthorInfo authorInfo = new ContentHotResponse.AuthorInfo();
                    if (content.getIsAnonymous()) {
                        authorInfo.setNickname("익명");
                        authorInfo.setIsAnonymous(true);
                    } else {
                        authorInfo.setNickname(content.getAuthor().getNickname());
                        authorInfo.setIsAnonymous(false);
                    }
                    item.setAuthor(authorInfo);
                    
                    return item;
                })
                .collect(Collectors.toList());
        
        return new ContentHotResponse(true, "인기 게시글 조회 성공", hotContentItems);
    }
}