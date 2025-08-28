package com.example.campung.global.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "감정 테스트 타입")
public enum EmotionTestType {
    
    @Schema(description = "우울한 분위기의 글")
    DEPRESS("depress.json", "우울한 분위기", "우울, 슬픔, 외로움, 막막함, 불안 등의 감정을 표현하는 글"),
    
    @Schema(description = "밝은 분위기의 글") 
    BRIGHT("bright.json", "밝은 분위기", "기쁨, 행복, 성취감, 만족감, 감사 등의 긍정적 감정을 표현하는 글"),
    
    @Schema(description = "무감정적이고 정보 전달 형식의 글")
    NON_EMOTION_INFO("nonEmotionInfo.json", "정보 전달", "공지사항, 안내, 분실물, 학사정보 등 객관적 정보를 전달하는 글"),
    
    @Schema(description = "화가 난 분위기의 글")
    ANGRY("angry.json", "화난 분위기", "분노, 짜증, 불만, 스트레스, 화 등의 부정적 감정을 표현하는 글"),
    
    @Schema(description = "흥분한 분위기의 글")
    EXCITE("excite.json", "흥분한 분위기", "설렘, 기대감, 흥미, 열정, 두근거림 등의 흥분 상태를 표현하는 글"),
    
    @Schema(description = "무작위 분위기의 글")
    RANDOM("random.json", "무작위 분위기", "일상적, 중성적, 복합감정, 평범한 내용의 글"),
    
    @Schema(description = "기존 test.json 파일 (전체 혼합)")
    ALL_MIXED("test.json", "전체 혼합", "모든 감정이 혼합된 기존 테스트 데이터");
    
    private final String fileName;
    private final String displayName;
    private final String description;
    
    EmotionTestType(String fileName, String displayName, String description) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 모든 감정 타입과 설명을 반환
     */
    public static String getAllTypesDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("사용 가능한 감정 테스트 타입:\n");
        for (EmotionTestType type : values()) {
            sb.append("• ").append(type.name())
              .append(" (").append(type.getDisplayName()).append("): ")
              .append(type.getDescription()).append("\n");
        }
        return sb.toString();
    }
}