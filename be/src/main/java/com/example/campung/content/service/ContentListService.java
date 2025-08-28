package com.example.campung.content.service;

import com.example.campung.content.dto.ContentListRequest;
import com.example.campung.content.dto.ContentListResponse;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.entity.Content;
import com.example.campung.global.enums.PostType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentListService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private ContentHotService contentHotService;
    
    public ContentListResponse getContentsByDate(ContentListRequest request) {
        System.out.println("=== CONTENT 리스트 조회 시작 ===");
        System.out.println("조회 날짜: " + request.getDate());
        System.out.println("게시글 타입: " + request.getPostType());
        System.out.println("위치 정보: lat=" + request.getLat() + ", lng=" + request.getLng() + ", radius=" + request.getRadius());
        
        validateDateRequest(request);
        
        List<Content> contents;
        
        // HOT 게시글인 경우 별도 처리
        if (request.getPostType() == PostType.HOT) {
            contents = contentHotService.getHotContent();
        } else {
            contents = contentRepository.findContentsByPostType(request.getPostType());
        }
        
        // HOT 게시글이 아닌 경우에만 날짜 필터링
        if (request.getPostType() != PostType.HOT) {
            LocalDate targetDate = LocalDate.parse(request.getDate());
            contents = contents.stream()
                .filter(content -> {
                    LocalDate contentDate = content.getCreatedAt().toLocalDate();
                    return contentDate.equals(targetDate);
                })
                .collect(Collectors.toList());
        }
        
        // Java에서 위치 필터링 (간단한 거리 계산)
        if (request.getLat() != null && request.getLng() != null) {
            final double userLat = request.getLat();
            final double userLng = request.getLng();
            final int radius = request.getRadius() != null ? request.getRadius() : 500; // 기본값 500m
            
            contents = contents.stream()
                .filter(content -> {
                    if (content.getLatitude() == null || content.getLongitude() == null) {
                        return false;
                    }
                    
                    double contentLat = content.getLatitude().doubleValue();
                    double contentLng = content.getLongitude().doubleValue();
                    
                    // 더 정확한 거리 계산 (Haversine 공식의 간단한 근사)
                    double latDiff = contentLat - userLat;
                    double lngDiff = contentLng - userLng;
                    
                    // 위도에 따른 경도 스케일링 적용
                    double avgLat = Math.toRadians((contentLat + userLat) / 2);
                    double latDistance = latDiff * 111.32; // 위도 1도 = 111.32km
                    double lngDistance = lngDiff * 111.32 * Math.cos(avgLat); // 경도는 위도에 따라 축소
                    
                    double distance = Math.sqrt(latDistance * latDistance + lngDistance * lngDistance);
                    
                    System.out.println("거리 계산: " + contentLat + "," + contentLng + 
                                     " → " + userLat + "," + userLng + 
                                     " = " + String.format("%.2f", distance) + "km (반경: " + radius + "km)");
                    
                    return distance <= radius;
                })
                .collect(Collectors.toList());
        }
        
        List<ContentListResponse.ContentListItem> contentItems = contents.stream()
            .map(this::convertToContentListItem)
            .collect(Collectors.toList());
        
        ContentListResponse.ListData listData = new ContentListResponse.ListData(
            request.getDate(),
            contentItems
        );
        
        System.out.println("조회 결과: " + contents.size() + "건");
        
        return new ContentListResponse(true, "날짜별 게시글 조회 성공", listData);
    }
    
    private void validateDateRequest(ContentListRequest request) {
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("조회할 날짜를 입력해주세요");
        }
        
        if (!request.getDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)");
        }
        
        // 위도, 경도가 있으면 위치 기반 검색 활성화
        if (request.getLat() != null || request.getLng() != null) {
            if (request.getLat() == null || request.getLng() == null) {
                throw new IllegalArgumentException("위치 기반 검색시 위도와 경도를 모두 입력해주세요");
            }
        }
    }
    
    private ContentListResponse.ContentListItem convertToContentListItem(Content content) {
        ContentListResponse.ContentListItem item = new ContentListResponse.ContentListItem();
        
        item.setContentId(content.getContentId());
        item.setPostType(content.getPostType().name());
        item.setTitle(content.getTitle());
        
        String displayNickname = content.getIsAnonymous() ? "익명" : content.getAuthor().getNickname();
        ContentListResponse.AuthorInfo author = new ContentListResponse.AuthorInfo(
            displayNickname,
            content.getIsAnonymous()
        );
        item.setAuthor(author);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        item.setCreatedAt(content.getCreatedAt().format(formatter) + "Z");
        
        // HOT 컨텐츠 여부 설정
        item.setHotContent(contentHotService.isHotContent(content.getContentId()));
        
        return item;
    }
}