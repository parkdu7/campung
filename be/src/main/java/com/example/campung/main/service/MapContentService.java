package com.example.campung.main.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.emotion.service.CampusEmotionService;
import com.example.campung.emotion.service.CampusTemperatureManager;
import com.example.campung.entity.Content;
import com.example.campung.entity.DailyCampus;
import com.example.campung.entity.Record;
import com.example.campung.global.enums.PostType;
import com.example.campung.global.enums.MarkerType;
import com.example.campung.main.dto.MapContentRequest;
import com.example.campung.record.repository.RecordRepository;
import com.example.campung.main.dto.MapContentResponse;
import com.example.campung.main.dto.MapContentResponse.MapContentData;
import com.example.campung.main.dto.MapContentResponse.MapContentItem;
import com.example.campung.main.dto.MapContentResponse.AuthorInfo;
import com.example.campung.main.dto.MapContentResponse.LocationInfo;
import com.example.campung.main.dto.MapContentResponse.MediaFileInfo;
import com.example.campung.main.dto.MapContentResponse.ReactionInfo;
import com.example.campung.main.dto.MapContentResponse.RecordItem;
import com.example.campung.global.util.CampusDateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MapContentService {

    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private RecordRepository recordRepository;
    
    @Autowired
    private CampusEmotionService campusEmotionService;
    
    @Autowired
    private CampusTemperatureManager temperatureManager;
    
    @Autowired
    private com.example.campung.content.service.ContentHotService contentHotService;

    public MapContentResponse getMapContents(MapContentRequest request) {
        System.out.println("=== 지도 콘텐츠 조회 시작 ===");
        System.out.println("lat: " + request.getLat() + ", lng: " + request.getLng());
        System.out.println("radius: " + request.getRadius() + ", postType: " + request.getPostType());
        System.out.println("date: " + request.getDate());

        // 캠퍼스 날짜 처리 (05:00-05:00 사이클)
        LocalDate targetCampusDate = CampusDateUtil.parseCampusDate(request.getDate());
        System.out.println("조회 대상 캠퍼스 날짜: " + targetCampusDate);
        System.out.println("캠퍼스 날짜 디버그: " + CampusDateUtil.debugCampusDate(LocalDateTime.now()));

        // 위치 기반 콘텐츠 조회 (위치와 캠퍼스 날짜 조건 적용)
        List<Content> contents = findContentsByLocationAndDate(request, targetCampusDate);
        System.out.println("조회된 콘텐츠 수: " + contents.size());

        // 위치 기반 녹음파일 조회
        List<Record> records = findRecordsByLocationAndDate(request, targetCampusDate);
        System.out.println("조회된 녹음파일 수: " + records.size());

        // DTO 변환
        List<MapContentItem> contentItems = contents.stream()
                .map(this::convertToMapContentItem)
                .collect(Collectors.toList());
                
        List<RecordItem> recordItems = records.stream()
                .map(this::convertToRecordItem)
                .collect(Collectors.toList());

        MapContentData data = new MapContentData(contentItems, recordItems);
        
        // 날짜별 온도/날씨 데이터 설정
        LocalDate todayCampusDate = CampusDateUtil.getCurrentCampusDate();
        if (targetCampusDate.equals(todayCampusDate)) {
            // 오늘 데이터: 현재 실시간 온도 및 감정 분석 결과 사용
            String emotionWeather = campusEmotionService.getCurrentEmotionWeather();
            Double currentTemperature = temperatureManager.getCurrentCampusTemperature();
            
            // 실시간 최고/최저 온도 조회
            double[] minMaxTemp = temperatureManager.getTodayMinMaxTemperature();
            double todayMaxTemp = minMaxTemp[0];
            double todayMinTemp = minMaxTemp[1];
            
            data.setEmotionWeather(emotionWeather);
            data.setEmotionTemperature(currentTemperature);
            data.setMaxTemperature(todayMaxTemp);
            data.setMinTemperature(todayMinTemp);
            
            System.out.println("오늘 데이터 - 날씨: " + emotionWeather + 
                             ", 현재온도: " + currentTemperature +
                             ", 최고온도: " + todayMaxTemp +
                             ", 최저온도: " + todayMinTemp);
        } else {
            // 과거 데이터: DailyCampus 테이블에서 조회
            DailyCampus dailyData = temperatureManager.getDailyCampusData(targetCampusDate);
            
            if (dailyData != null) {
                // DailyCampus 데이터가 있는 경우: 최저, 최고, 평균온도, 마지막 날씨 반환
                data.setEmotionWeather(dailyData.getWeatherType().name().toLowerCase());
                data.setEmotionTemperature(dailyData.getFinalTemperature()); // 평균온도
                data.setMaxTemperature(dailyData.getMaxTemperature());
                data.setMinTemperature(dailyData.getMinTemperature());
                
                System.out.println("과거 데이터 (" + targetCampusDate + ") - 날씨: " + dailyData.getWeatherType().name().toLowerCase() + 
                                 ", 평균온도: " + dailyData.getFinalTemperature() +
                                 ", 최고온도: " + dailyData.getMaxTemperature() +
                                 ", 최저온도: " + dailyData.getMinTemperature());
            } else {
                // 데이터가 없는 경우 기본값
                data.setEmotionWeather("cloudy");
                data.setEmotionTemperature(20.0);
                data.setMaxTemperature(25.0);
                data.setMinTemperature(15.0);
                
                System.out.println("과거 데이터 없음 (" + targetCampusDate + ") - 기본값 사용");
            }
        }
        
        return new MapContentResponse(true, "지도 콘텐츠 조회 성공", data);
    }

    private LocalDate parseDate(String dateStr) {
        return CampusDateUtil.parseCampusDate(dateStr);
    }

    private List<Content> findContentsByLocationAndDate(MapContentRequest request, LocalDate targetCampusDate) {
        double userLat = request.getLat();
        double userLng = request.getLng();
        int radiusInMeters = request.getRadius();
        PostType postType = request.getPostType();

        System.out.println("위치 기반 검색: lat=" + userLat + ", lng=" + userLng + ", radius=" + radiusInMeters + "m");

        // 넓은 범위로 DB에서 조회 (반경의 2배 정도)
        double radiusInKm = radiusInMeters / 1000.0;
        double searchRadiusInDegrees = (radiusInKm * 2) / 111.32; // 2배 넓게 검색

        double minLat = userLat - searchRadiusInDegrees;
        double maxLat = userLat + searchRadiusInDegrees;
        double minLng = userLng - searchRadiusInDegrees / Math.cos(Math.toRadians(userLat));
        double maxLng = userLng + searchRadiusInDegrees / Math.cos(Math.toRadians(userLat));

        System.out.println("DB 검색 범위 - lat: " + minLat + "~" + maxLat + ", lng: " + minLng + "~" + maxLng);

        // 캠퍼스 날짜 범위 설정 (05:00 ~ 다음날 04:59:59)
        LocalDateTime startDateTime = CampusDateUtil.getCampusDateStartTime(targetCampusDate);
        LocalDateTime endDateTime = CampusDateUtil.getCampusDateEndTime(targetCampusDate);
        
        System.out.println("캠퍼스 날짜 범위: " + startDateTime + " ~ " + endDateTime);

        List<Content> contents;
        if (postType != null && postType == PostType.HOT) {
            // HOT postType 요청 시 isHot = true인 게시글 조회
            contents = contentRepository.findByLocationAndDateAndIsHot(
                    minLat, maxLat, minLng, maxLng, startDateTime, endDateTime
            );
        } else if (postType != null) {
            contents = contentRepository.findByLocationAndDateAndPostType(
                    minLat, maxLat, minLng, maxLng, startDateTime, endDateTime, postType
            );
        } else {
            contents = contentRepository.findByLocationAndDate(
                    minLat, maxLat, minLng, maxLng, startDateTime, endDateTime
            );
        }

        System.out.println("DB에서 조회된 콘텐츠 수: " + contents.size());

        // Java에서 정확한 거리 계산으로 필터링
        List<Content> filteredContents = contents.stream()
                .filter(content -> {
                    if (content.getLatitude() == null || content.getLongitude() == null) {
                        return false;
                    }

                    double contentLat = content.getLatitude().doubleValue();
                    double contentLng = content.getLongitude().doubleValue();

                    // Haversine 공식 기반 거리 계산
                    double latDiff = contentLat - userLat;
                    double lngDiff = contentLng - userLng;

                    double avgLat = Math.toRadians((contentLat + userLat) / 2);
                    double latDistance = latDiff * 111.32; // km
                    double lngDistance = lngDiff * 111.32 * Math.cos(avgLat); // km

                    double distanceInKm = Math.sqrt(latDistance * latDistance + lngDistance * lngDistance);
                    double distanceInMeters = distanceInKm * 1000;

                    System.out.println("거리 계산: " + contentLat + "," + contentLng + 
                                     " → " + userLat + "," + userLng + 
                                     " = " + String.format("%.0f", distanceInMeters) + "m (반경: " + radiusInMeters + "m)");

                    return distanceInMeters <= radiusInMeters;
                })
                .collect(Collectors.toList());

        System.out.println("반경 내 필터링된 콘텐츠 수: " + filteredContents.size());
        
        return filteredContents;
    }
    
    private List<Record> findRecordsByLocationAndDate(MapContentRequest request, LocalDate targetCampusDate) {
        double userLat = request.getLat();
        double userLng = request.getLng();
        int radiusInMeters = request.getRadius();

        System.out.println("녹음파일 위치 기반 검색: lat=" + userLat + ", lng=" + userLng + ", radius=" + radiusInMeters + "m");

        // 넓은 범위로 DB에서 조회 (반경의 2배 정도)
        double radiusInKm = radiusInMeters / 1000.0;
        double searchRadiusInDegrees = (radiusInKm * 2) / 111.32; // 2배 넓게 검색

        double minLat = userLat - searchRadiusInDegrees;
        double maxLat = userLat + searchRadiusInDegrees;
        double minLng = userLng - searchRadiusInDegrees / Math.cos(Math.toRadians(userLat));
        double maxLng = userLng + searchRadiusInDegrees / Math.cos(Math.toRadians(userLat));

        // 캠퍼스 날짜 범위 설정 (05:00 ~ 다음날 04:59:59)
        LocalDateTime startDateTime = CampusDateUtil.getCampusDateStartTime(targetCampusDate);
        LocalDateTime endDateTime = CampusDateUtil.getCampusDateEndTime(targetCampusDate);

        // 모든 녹음파일 조회 후 Java에서 필터링
        List<Record> allRecords = recordRepository.findAll();
        
        List<Record> filteredRecords = allRecords.stream()
                .filter(record -> {
                    // 날짜 필터링
                    if (record.getCreatedAt().isBefore(startDateTime) || 
                        record.getCreatedAt().isAfter(endDateTime)) {
                        return false;
                    }

                    // 위치 필터링
                    if (record.getLatitude() == null || record.getLongitude() == null) {
                        return false;
                    }

                    double recordLat = record.getLatitude().doubleValue();
                    double recordLng = record.getLongitude().doubleValue();

                    // Haversine 공식 기반 거리 계산
                    double latDiff = recordLat - userLat;
                    double lngDiff = recordLng - userLng;

                    double avgLat = Math.toRadians((recordLat + userLat) / 2);
                    double latDistance = latDiff * 111.32; // km
                    double lngDistance = lngDiff * 111.32 * Math.cos(avgLat); // km

                    double distanceInKm = Math.sqrt(latDistance * latDistance + lngDistance * lngDistance);
                    double distanceInMeters = distanceInKm * 1000;

                    System.out.println("녹음파일 거리 계산: " + recordLat + "," + recordLng + 
                                     " → " + userLat + "," + userLng + 
                                     " = " + String.format("%.0f", distanceInMeters) + "m (반경: " + radiusInMeters + "m)");

                    return distanceInMeters <= radiusInMeters;
                })
                .collect(Collectors.toList());

        System.out.println("반경 내 필터링된 녹음파일 수: " + filteredRecords.size());
        
        return filteredRecords;
    }

    private MapContentItem convertToMapContentItem(Content content) {
        MapContentItem item = new MapContentItem();
        
        item.setContentId(content.getContentId());
        item.setUserId(content.getAuthor().getUserId());
        
        // Author 정보
        String displayNickname = content.getIsAnonymous() ? "익명" : content.getAuthor().getNickname();
        AuthorInfo author = new AuthorInfo(
                displayNickname,
                content.getIsAnonymous()
        );
        item.setAuthor(author);

        // Location 정보
        if (content.getLatitude() != null && content.getLongitude() != null) {
            LocationInfo location = new LocationInfo(
                    content.getLatitude().doubleValue(),
                    content.getLongitude().doubleValue()
            );
            item.setLocation(location);
        }

        // PostType 정보 (HOT 게시글 체크)
        boolean isHotContent = contentHotService.isHotContent(content.getContentId());
        System.out.println("Content ID " + content.getContentId() + " - isHot: " + isHotContent + 
                          ", originalPostType: " + content.getPostType().name());
        
        if (isHotContent) {
            // HOT 게시글인 경우 postType을 HOT으로 변경
            item.setPostType("HOT");
            item.setPostTypeName("인기글");
            item.setMarkerType(MarkerType.fromPostType(com.example.campung.global.enums.PostType.HOT).getMarkerType());
            System.out.println("→ HOT 게시글로 변경됨");
        } else {
            // 일반 게시글인 경우 원래 postType 사용
            item.setPostType(content.getPostType().name());
            item.setPostTypeName(content.getPostType().getDescription());
            item.setMarkerType(MarkerType.fromPostType(content.getPostType()).getMarkerType());
        }
        
        // ContentScope는 현재 구현에서는 모두 "MAP"으로 설정
        item.setContentScope("MAP");
        
        // Content 상세 정보
        item.setContentType(determineContentTypeFromUrl(content));
        item.setTitle(content.getTitle());
        item.setBody(content.getContent());
        item.setEmotionTag(content.getEmotion());
        
        // MediaFiles 정보 (썸네일 URL만)
        if (content.getAttachments() != null && !content.getAttachments().isEmpty()) {
            List<MediaFileInfo> mediaFiles = content.getAttachments().stream()
                    .map(attachment -> new MediaFileInfo(attachment.getThumbnailUrl()))
                    .collect(Collectors.toList());
            item.setMediaFiles(mediaFiles);
        }
        
        // Reactions 정보
        ReactionInfo reactions = new ReactionInfo(
                content.getLikeCount() != null ? content.getLikeCount() : 0,
                content.getCommentCount() != null ? content.getCommentCount() : 0
        );
        item.setReactions(reactions);
        
        // 날짜 정보
        item.setCreatedAt(content.getCreatedAt().toString() + "Z");
        item.setExpiresAt(content.getCreatedAt().plusDays(7).toString() + "Z"); // 7일 후 만료

        return item;
    }
    
    private String determineContentTypeFromUrl(Content content) {
        if (content.getAttachments() != null && !content.getAttachments().isEmpty()) {
            // 첫 번째 첨부파일의 URL로 디렉토리 구조 확인
            String firstFileUrl = content.getAttachments().get(0).getUrl();
            if (firstFileUrl != null) {
                if (firstFileUrl.contains("/images/")) {
                    return "PHOTO";
                } else if (firstFileUrl.contains("/videos/")) {
                    return "VIDEO";
                } else if (firstFileUrl.contains("/audios/")) {
                    return "AUDIO";
                }
            }
        }
        return "TEXT"; // 첨부파일이 없으면 텍스트
    }
    
    private RecordItem convertToRecordItem(Record record) {
        RecordItem item = new RecordItem();
        
        item.setRecordId(record.getRecordId());
        item.setUserId(record.getUser().getUserId());
        
        // Author 정보
        AuthorInfo author = new AuthorInfo(
                record.getUser().getNickname(),
                false // 익명 여부는 추후 구현
        );
        item.setAuthor(author);

        // Location 정보
        if (record.getLatitude() != null && record.getLongitude() != null) {
            LocationInfo location = new LocationInfo(
                    record.getLatitude().doubleValue(),
                    record.getLongitude().doubleValue()
            );
            item.setLocation(location);
        }
        
        // Record URL
        item.setRecordUrl(record.getRecordUrl());
        
        // 날짜 정보
        item.setCreatedAt(record.getCreatedAt().toString() + "Z");

        return item;
    }
    
    public String debugAllContents() {
        List<Content> allContents = contentRepository.findAll();
        StringBuilder result = new StringBuilder();
        
        result.append("=== 전체 콘텐츠 조회 결과 ===\n");
        result.append("총 콘텐츠 수: ").append(allContents.size()).append("\n\n");
        
        for (Content content : allContents) {
            result.append("ID: ").append(content.getContentId())
                  .append(", 제목: ").append(content.getTitle())
                  .append(", 타입: ").append(content.getPostType())
                  .append(", 생성일: ").append(content.getCreatedAt())
                  .append(", 위치: ").append(content.getLatitude()).append(",").append(content.getLongitude())
                  .append("\n");
        }
        
        return result.toString();
    }
    
    public String debugContentsByDate(String postType, String date) {
        PostType postTypeEnum = null;
        if (postType != null && !postType.trim().isEmpty()) {
            try {
                postTypeEnum = PostType.valueOf(postType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "유효하지 않은 게시글 타입: " + postType;
            }
        }
        
        LocalDate targetCampusDate = parseDate(date);
        LocalDateTime startDateTime = CampusDateUtil.getCampusDateStartTime(targetCampusDate);
        LocalDateTime endDateTime = CampusDateUtil.getCampusDateEndTime(targetCampusDate);
        
        List<Content> contents;
        if (postTypeEnum != null) {
            contents = contentRepository.findContentsByPostType(postTypeEnum);
        } else {
            contents = contentRepository.findAll();
        }
        
        // 날짜 필터링
        contents = contents.stream()
                .filter(content -> {
                    return content.getCreatedAt().isAfter(startDateTime) && 
                           content.getCreatedAt().isBefore(endDateTime);
                })
                .collect(Collectors.toList());
        
        StringBuilder result = new StringBuilder();
        result.append("=== 날짜별 콘텐츠 조회 결과 ===\n");
        result.append("날짜: ").append(date).append("\n");
        result.append("타입: ").append(postType != null ? postType : "ALL").append("\n");
        result.append("조회된 콘텐츠 수: ").append(contents.size()).append("\n\n");
        
        for (Content content : contents) {
            result.append("ID: ").append(content.getContentId())
                  .append(", 제목: ").append(content.getTitle())
                  .append(", 타입: ").append(content.getPostType())
                  .append(", 생성일: ").append(content.getCreatedAt())
                  .append(", 위치: ").append(content.getLatitude()).append(",").append(content.getLongitude())
                  .append("\n");
        }
        
        return result.toString();
    }

}