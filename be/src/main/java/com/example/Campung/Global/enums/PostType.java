package com.example.campung.global.enums;

public enum PostType {
    NOTICE("공지게시판"),
    INFO("정보게시판"),
    MARKET("장터게시판"),
    FREE("자유게시판"),
    SECRET("비밀게시판"),
    HOT("인기게시판");
    
    private final String description;
    
    PostType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static PostType fromString(String postType) {
        if (postType == null) {
            return null;
        }
        
        try {
            return PostType.valueOf(postType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 게시글 타입입니다: " + postType);
        }
    }
}