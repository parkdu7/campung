package com.example.campung.record.controller;

import com.example.campung.record.dto.RecordCreateRequest;
import com.example.campung.record.dto.RecordCreateResponse;
import com.example.campung.record.dto.RecordDeleteResponse;
import com.example.campung.record.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/records")
@Tag(name = "Record", description = "녹음파일 관리 API")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @Operation(
        summary = "녹음파일 등록", 
        description = "새 녹음파일을 업로드하고 등록합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        requestBody = @RequestBody(
                content = @Content(
                        mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                        schema = @Schema(implementation = RecordCreateRequest.class)
                )
        )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordCreateResponse> createRecord(
            @Parameter(description = "인증 토큰", example = "Bearer test", required = true)
            @RequestHeader("Authorization") String authorization,
            @ModelAttribute RecordCreateRequest request) throws IOException {

        // 인증 토큰 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            RecordCreateResponse errorResponse = new RecordCreateResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }

        // 오디오 파일 검증
        if (request.getAudioFile() == null || request.getAudioFile().isEmpty()) {
            RecordCreateResponse errorResponse = new RecordCreateResponse(false, "녹음 파일이 필요합니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 위치 정보 검증 (선택사항이지만 값이 있으면 유효성 검사)
        if (request.getLatitude() != null && request.getLongitude() != null) {
            if (request.getLatitude() < -90 || request.getLatitude() > 90 || 
                request.getLongitude() < -180 || request.getLongitude() > 180) {
                RecordCreateResponse errorResponse = new RecordCreateResponse(false, "유효하지 않은 위도/경도 값입니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }

        String accessToken = authorization.substring(7);
        RecordCreateResponse response = recordService.createRecord(request, accessToken);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
        summary = "녹음파일 삭제", 
        description = "지정된 녹음파일을 삭제합니다. 본인이 등록한 파일만 삭제 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{recordId}")
    public ResponseEntity<RecordDeleteResponse> deleteRecord(
            @Parameter(description = "삭제할 녹음파일 ID", required = true)
            @PathVariable Long recordId,
            @Parameter(description = "인증 토큰", example = "Bearer test", required = true)
            @RequestHeader("Authorization") String authorization) {

        // 인증 토큰 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            RecordDeleteResponse errorResponse = new RecordDeleteResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }

        // recordId 검증
        if (recordId == null || recordId <= 0) {
            RecordDeleteResponse errorResponse = new RecordDeleteResponse(false, "유효하지 않은 녹음파일 ID입니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String accessToken = authorization.substring(7);
        RecordDeleteResponse response = recordService.deleteRecord(recordId, accessToken);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}