package com.example.campung.main.controller;

import com.example.campung.global.enums.PostType;
import com.example.campung.main.dto.MapContentRequest;
import com.example.campung.main.dto.MapContentResponse;
import com.example.campung.main.service.MapContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/map")
@Tag(name = "Map Content", description = "지도 콘텐츠 API")
public class MapContentController {

    @Autowired
    private MapContentService mapContentService;

    @Operation(summary = "지도 콘텐츠 조회", description = "위치 기반으로 주변 콘텐츠를 조회합니다.")
    @GetMapping("/contents")
    public ResponseEntity<MapContentResponse> getMapContents(
            @Parameter(description = "중심 위도", required = true)
            @RequestParam Double lat,
            
            @Parameter(description = "중심 경도", required = true) 
            @RequestParam Double lng,
            
            @Parameter(description = "반경(미터), 기본값: 500")
            @RequestParam(defaultValue = "500") Integer radius,
            
            @Parameter(description = "게시글 타입 필터")
            @RequestParam(required = false) String postType,
            
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD), 기본값: 오늘")
            @RequestParam(required = false) String date) {

        // 파라미터 검증
        if (lat == null || lng == null) {
            MapContentResponse errorResponse = new MapContentResponse(false, "위도와 경도는 필수 파라미터입니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            MapContentResponse errorResponse = new MapContentResponse(false, "유효하지 않은 위도/경도 값입니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (radius != null && radius < 0) {
            MapContentResponse errorResponse = new MapContentResponse(false, "반경은 0 이상이어야 합니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // PostType 검증
        PostType postTypeEnum = null;
        if (postType != null && !postType.trim().isEmpty() && !postType.equalsIgnoreCase("ALL")) {
            try {
                postTypeEnum = PostType.valueOf(postType.toUpperCase());
            } catch (IllegalArgumentException e) {
                MapContentResponse errorResponse = new MapContentResponse(false, "유효하지 않은 게시글 타입입니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }

        // 날짜가 없으면 오늘 날짜로 설정
        if (date == null || date.trim().isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        // Request 객체 생성
        MapContentRequest request = new MapContentRequest();
        request.setLat(lat);
        request.setLng(lng);
        request.setRadius(radius);
        request.setPostType(postTypeEnum);
        request.setDate(date);

        // 서비스 호출
        MapContentResponse response = mapContentService.getMapContents(request);

        return ResponseEntity.ok(response);
    }
}