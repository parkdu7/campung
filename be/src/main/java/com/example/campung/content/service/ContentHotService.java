package com.example.campung.content.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.content.dto.ContentHotResponse;
import com.example.campung.entity.Content;
import com.example.campung.entity.ContentHot;
import com.example.campung.content.repository.ContentHotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContentHotService {
    
    @Value("${app.default-profile-image-url}")
    private String defaultProfileImageUrl;
    
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
        
        // 새로운 HOT 컨텐츠 저장 (중복 체크)
        for (Object contentIdObj : hotContentIds) {
            Long contentId = Long.valueOf(contentIdObj.toString());
            long hotScore = contentHotTrackingService.getLike24hCount(contentId);
            
            // 최소 좋아요 수 조건 (예: 5개 이상)
            if (hotScore >= 5) {
                // 이미 등록된 핫 게시글인지 확인
                if (!contentHotRepository.existsByContentId(contentId)) {
                    contentRepository.findById(contentId).ifPresent(content -> {
                        ContentHot contentHot = ContentHot.builder()
                                .contentId(contentId)
                                .content(content)
                                .hotScore(hotScore)
                                .build();
                        contentHotRepository.save(contentHot);
                        
                        // Content의 isHot 플래그를 true로 설정
                        content.setIsHot(true);
                        contentRepository.save(content);
                    });
                } else {
                    // 이미 등록된 게시글은 hotScore만 업데이트
                    contentHotRepository.findByContentId(contentId).ifPresent(existingHot -> {
                        existingHot.setHotScore(hotScore);
                        contentHotRepository.save(existingHot);
                    });
                }
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
        
        // 현재 캠퍼스 사이클(05시 기준) 내의 HOT 게시글만 필터링
        LocalDateTime currentCycleStart = getCurrentCycleStart();
        
        List<ContentHotResponse.HotContentItem> hotContentItems = hotContents.stream()
                .filter(contentHot -> {
                    LocalDateTime createdAt = contentHot.getContent().getCreatedAt();
                    return !createdAt.isBefore(currentCycleStart);
                })
                .map(contentHot -> {
                    Content content = contentHot.getContent();
                    ContentHotResponse.HotContentItem item = new ContentHotResponse.HotContentItem();
                    
                    item.setContentId(content.getContentId());
                    item.setUserId(content.getAuthor() != null ? content.getAuthor().getUserId() : null);
                    item.setTitle(content.getTitle());
                    item.setContent(content.getContent());
                    item.setPostType(content.getPostType().name());
                    item.setCreatedAt(content.getCreatedAt().toString());
                    item.setHotScore(contentHot.getHotScore());
                    item.setLikeCount(content.getLikeCount());
                    item.setCommentCount(content.getCommentCount());
                    item.setBuildingName(content.getBuildingName());
                    item.setEmotion(content.getEmotion());
                    item.setThumbnailUrl(content.getAttachments() != null && !content.getAttachments().isEmpty() ? 
                        content.getAttachments().get(0).getThumbnailUrl() : null);
                    
                    String profileImageUrl = content.getAuthor() != null ? content.getAuthor().getProfileImageUrl() : null;
                    if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
                        profileImageUrl = defaultProfileImageUrl;
                    }
                    item.setUserProfileUrl(profileImageUrl);
                    
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
    
    private LocalDateTime getCurrentCycleStart() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime fiveAm = LocalTime.of(5, 0);
        
        if (now.toLocalTime().isBefore(fiveAm)) {
            // 현재 시간이 05:00 이전이면 전날 05:00부터
            return now.minusDays(1).with(fiveAm);
        } else {
            // 현재 시간이 05:00 이후면 당일 05:00부터
            return now.with(fiveAm);
        }
    }
}