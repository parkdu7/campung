package com.example.campung.lankmark.dto;

import com.example.campung.global.enums.LandmarkCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "랜드마크 등록 요청 (Form Data)")
public class LandmarkCreateFormRequest {
    
    @NotBlank(message = "랜드마크 이름은 필수입니다")
    @Size(max = 100, message = "랜드마크 이름은 100자 이내로 입력해주세요")
    @Schema(description = "랜드마크 이름", example = "중앙도서관")
    private String name;
    
    @Size(max = 500, message = "랜드마크 설명은 500자 이내로 입력해주세요")
    @Schema(description = "랜드마크 설명", example = "24시간 운영하는 메인 도서관")
    private String description;
    
    @NotNull(message = "위도는 필수입니다")
    @Schema(description = "위도", example = "36.0")
    private Double latitude;
    
    @NotNull(message = "경도는 필수입니다")
    @Schema(description = "경도", example = "127.0")
    private Double longitude;
    
    @NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "랜드마크 카테고리", example = "LIBRARY")
    private LandmarkCategory category;
    
    @Schema(description = "이미지 파일")
    private MultipartFile imageFile;
}