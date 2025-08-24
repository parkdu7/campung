package com.example.Campung.Content.Service;

import com.example.Campung.Content.Dto.ContentListRequest;
import com.example.Campung.Content.Dto.ContentListResponse;
import com.example.Campung.Content.Repository.ContentRepository;
import com.example.Campung.Entity.Content;
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
    
    public ContentListResponse getContentsByDate(ContentListRequest request) {
        System.out.println("=== CONTENT 리스트 조회 시작 ===");
        System.out.println("조회 날짜: " + request.getDate());
        System.out.println("게시글 타입: " + request.getPostType());
        System.out.println("위치 정보: lat=" + request.getLat() + ", lng=" + request.getLng() + ", radius=" + request.getRadius());
        
        validateDateRequest(request);
        
        List<Content> contents = contentRepository.findContentsByPostType(request.getPostType());
        
        // Java에서 날짜 필터링
        LocalDate targetDate = LocalDate.parse(request.getDate());
        contents = contents.stream()
            .filter(content -> {
                LocalDate contentDate = content.getCreatedAt().toLocalDate();
                return contentDate.equals(targetDate);
            })
            .collect(Collectors.toList());
        
        // Java에서 위치 필터링 (간단한 거리 계산)
        if (request.getLat() != null && request.getLng() != null && request.getRadius() != null) {
            final double userLat = request.getLat();
            final double userLng = request.getLng();
            final int radius = request.getRadius();
            
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
        
        if ((request.getLat() != null || request.getLng() != null || request.getRadius() != null)) {
            if (request.getLat() == null || request.getLng() == null || request.getRadius() == null) {
                throw new IllegalArgumentException("위치 기반 검색시 위도, 경도, 반경을 모두 입력해주세요");
            }
        }
    }
    
    private ContentListResponse.ContentListItem convertToContentListItem(Content content) {
        ContentListResponse.ContentListItem item = new ContentListResponse.ContentListItem();
        
        item.setContentId(content.getContentId());
        item.setPostType(content.getPostType().name());
        item.setTitle(content.getTitle());
        
        ContentListResponse.AuthorInfo author = new ContentListResponse.AuthorInfo(
            content.getAuthor().getNickname(),
            false
        );
        item.setAuthor(author);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        item.setCreatedAt(content.getCreatedAt().format(formatter) + "Z");
        
        return item;
    }
}