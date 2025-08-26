package com.example.campung.global.enums;

public enum MarkerType {
    NOTICE_MARKER("notice_marker"),
    INFO_MARKER("info_marker"),
    MARKET_MARKER("market_marker"),
    FREE_MARKER("free_marker"),
    SECRET_MARKER("secret_marker"),
    HOT_MARKER("hot_marker"),
    DEFAULT_MARKER("default_marker");
    
    private final String markerType;
    
    MarkerType(String markerType) {
        this.markerType = markerType;
    }
    
    public String getMarkerType() {
        return markerType;
    }
    
    public static MarkerType fromPostType(PostType postType) {
        if (postType == null) {
            return DEFAULT_MARKER;
        }
        
        switch (postType) {
            case NOTICE: return NOTICE_MARKER;
            case INFO: return INFO_MARKER;
            case MARKET: return MARKET_MARKER;
            case FREE: return FREE_MARKER;
            case SECRET: return SECRET_MARKER;
            case HOT: return HOT_MARKER;
            default: return DEFAULT_MARKER;
        }
    }
}