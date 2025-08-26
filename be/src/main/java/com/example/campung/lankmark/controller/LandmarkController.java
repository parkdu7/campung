package com.example.campung.lankmark.controller;

import com.example.campung.lankmark.dto.LandmarkCreateResponse;
import com.example.campung.lankmark.dto.LandmarkCreateFormRequest;
import com.example.campung.lankmark.dto.LandmarkUpdateRequest;
import com.example.campung.lankmark.dto.LandmarkUpdateResponse;
import com.example.campung.lankmark.dto.LandmarkDetailResponse;
import com.example.campung.lankmark.dto.MapPOIRequest;
import com.example.campung.lankmark.dto.MapPOIResponse;
import com.example.campung.lankmark.service.LandmarkCrudService;
import com.example.campung.lankmark.service.LandmarkSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/landmark")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Landmark", description = "랜드마크 관리 API")
public class LandmarkController {

    private final LandmarkCrudService crudService;
    private final LandmarkSearchService searchService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "랜드마크 등록 (Form Data)", 
               description = "새로운 랜드마크를 등록합니다. 이미지 파일과 함께 form-data로 전송하면 자동으로 썸네일이 생성됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "랜드마크 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 파라미터 누락, 잘못된 좌표)"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (관리자 권한 필요)"),
        @ApiResponse(responseCode = "409", description = "중복된 랜드마크 (같은 위치에 이미 존재)"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiSuccessResponse<LandmarkCreateResponse>> createLandmarkWithFormData(
            @ModelAttribute @Valid LandmarkCreateFormRequest formRequest) {
        
        LandmarkCreateResponse response = crudService.createLandmarkWithFormRequest(formRequest);
        
        log.info("랜드마크 등록 성공: {} (위치: {}, {})", 
                formRequest.getName(), formRequest.getLatitude(), formRequest.getLongitude());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(response));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "랜드마크 수정 (Form Data)", 
               description = "기존 랜드마크를 수정합니다. 새 이미지 파일을 제공하면 자동으로 썸네일도 새로 생성됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "랜드마크 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 파라미터 누락, 잘못된 좌표)"),
        @ApiResponse(responseCode = "404", description = "랜드마크를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "중복된 랜드마크 (같은 위치에 이미 존재)"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiSuccessResponse<LandmarkUpdateResponse>> updateLandmark(
            @Parameter(description = "랜드마크 ID") @PathVariable Long id,
            @ModelAttribute @Valid LandmarkUpdateRequest updateRequest) {
        
        LandmarkUpdateResponse response = crudService.updateLandmark(id, updateRequest);
        
        log.info("랜드마크 수정 성공: {} (ID: {})", 
                updateRequest.getName(), id);
        
        return ResponseEntity.ok(ApiSuccessResponse.of(response));
    }

    @GetMapping("/map")
    @Operation(summary = "맵 POI 목록 조회", 
               description = "위치 기반으로 주변 랜드마크(POI) 목록을 조회합니다. 썸네일, 위도, 경도, 이름 정보를 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "맵 POI 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 파라미터 누락, 잘못된 좌표)"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<MapPOIResponse> getMapPOIs(
            @Parameter(description = "위도") @RequestParam Double lat,
            @Parameter(description = "경도") @RequestParam Double lng,
            @Parameter(description = "검색 반경(미터), 기본값: 1000") @RequestParam(required = false) Integer radius,
            @Parameter(description = "카테고리 필터 (선택사항)") @RequestParam(required = false) String category) {
        
        MapPOIRequest request = MapPOIRequest.builder()
            .latitude(lat)
            .longitude(lng)
            .radius(radius)
            .category(category)
            .build();
        
        MapPOIResponse response = searchService.getMapPOIs(request);
        
        log.info("맵 POI 조회 성공: {}개 (위치: {}, {}, 반경: {}m)", 
                response.getData().size(), lat, lng, radius != null ? radius : 1000);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "POI 요약 조회 (상세)", 
               description = "지정된 ID의 랜드마크 상세 정보를 조회합니다. 이름, 설명, 이미지 URL, 요약 정보를 포함합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "POI 상세 조회 성공"),
        @ApiResponse(responseCode = "404", description = "랜드마크를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiSuccessResponse<LandmarkDetailResponse>> getPOISummary(
            @Parameter(description = "랜드마크 ID") @PathVariable Long id) {
        
        LandmarkDetailResponse response = crudService.getLandmarkDetail(id);
        
        log.info("POI 상세 조회 성공: {} (ID: {})", response.getName(), id);
        
        return ResponseEntity.ok(ApiSuccessResponse.of(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "랜드마크 삭제", 
               description = "지정된 ID의 랜드마크를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "랜드마크 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "랜드마크를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiSuccessResponse<String>> deleteLandmark(
            @Parameter(description = "랜드마크 ID") @PathVariable Long id) {
        
        crudService.deleteLandmark(id);
        
        log.info("랜드마크 삭제 성공: ID {}", id);
        
        return ResponseEntity.ok(ApiSuccessResponse.of("랜드마크가 삭제되었습니다"));
    }


    // API 응답 래퍼 클래스들
    @Getter
    public static class ApiSuccessResponse<T> {
        private final boolean success = true;
        private final T data;

        private ApiSuccessResponse(T data) {
            this.data = data;
        }

        public static <T> ApiSuccessResponse<T> of(T data) {
            return new ApiSuccessResponse<>(data);
        }

    }

    @Getter
    public static class ApiErrorResponse {
        private final boolean success = false;
        private final String errorCode;
        private final String message;

        private ApiErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public static ApiErrorResponse of(String errorCode, String message) {
            return new ApiErrorResponse(errorCode, message);
        }

    }
}