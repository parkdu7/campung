package com.shinhan.campung.presentation.ui.components.bottomsheet

/**
 * 바텀시트의 가능한 상태값들
 */
enum class BottomSheetValue {
    /**
     * 바텀시트가 완전히 숨겨진 상태
     */
    Hidden,
    
    /**
     * 바텀시트가 부분적으로 확장된 상태 (peek 상태)
     */
    PartiallyExpanded,
    
    /**
     * 바텀시트가 완전히 확장된 상태
     */
    Expanded
}