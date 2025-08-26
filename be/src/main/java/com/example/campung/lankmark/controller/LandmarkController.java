package com.example.campung.lankmark.controller;

import com.example.campung.lankmark.dto.LandmarkCreateResponse;
import com.example.campung.lankmark.dto.LandmarkCreateFormRequest;
import com.example.campung.lankmark.service.LandmarkService;
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

    private final LandmarkService landmarkService;

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
        
        LandmarkCreateResponse response = landmarkService.createLandmarkWithFormRequest(formRequest);
        
        log.info("랜드마크 등록 성공: {} (위치: {}, {})", 
                formRequest.getName(), formRequest.getLatitude(), formRequest.getLongitude());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(response));
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