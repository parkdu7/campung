package com.shinhan.campung.data.model

import java.time.LocalDateTime

/**
 * 지도에 표시되는 항목들의 공통 인터페이스 (Content와 Record 통합)
 */
sealed interface MapItem {
    val id: Long
    val location: Location
    val createdAt: String
    val createdAtDateTime: LocalDateTime
    val type: MapItemType
    val title: String
    val author: Author
}

enum class MapItemType {
    CONTENT, RECORD
}

/**
 * MapContent를 MapItem으로 래핑하는 구현체
 */
data class MapContentItem(
    val content: MapContent
) : MapItem {
    override val id: Long get() = content.contentId
    override val location: Location get() = content.location
    override val createdAt: String get() = content.createdAt
    override val createdAtDateTime: LocalDateTime get() = content.createdAtDateTime
    override val type: MapItemType get() = MapItemType.CONTENT
    override val title: String get() = content.title
    override val author: Author get() = content.author
}

/**
 * MapRecord를 MapItem으로 래핑하는 구현체
 */
data class MapRecordItem(
    val record: MapRecord
) : MapItem {
    override val id: Long get() = record.recordId
    override val location: Location get() = record.location
    override val createdAt: String get() = record.createdAt
    override val createdAtDateTime: LocalDateTime get() = parseDateTime(record.createdAt)
    override val type: MapItemType get() = MapItemType.RECORD
    override val title: String get() = "녹음 - ${record.author.nickname}" // Record는 제목이 없으므로 생성
    override val author: Author get() = record.author
    
    // MapRecord용 날짜 파싱 (MapContent와 동일한 로직)
    private fun parseDateTime(dateStr: String): LocalDateTime {
        return try {
            val cleanDateStr = dateStr.removeSuffix("Z")
            val truncatedStr = if (cleanDateStr.contains(".") && cleanDateStr.substringAfter(".").length > 3) {
                val beforeDot = cleanDateStr.substringBefore(".")
                val afterDot = cleanDateStr.substringAfter(".")
                val truncatedMicros = afterDot.take(3)
                "$beforeDot.$truncatedMicros"
            } else {
                cleanDateStr
            }
            LocalDateTime.parse(truncatedStr)
        } catch (e: Exception) {
            // 한국 현재 시간으로 fallback
            java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime()
        }
    }
}

/**
 * MapItem 확장 함수들
 */
fun List<MapItem>.sortedByCreatedAt(): List<MapItem> {
    return this.sortedByDescending { it.createdAtDateTime }
}

fun List<MapContent>.toMapItems(): List<MapItem> {
    return this.map { MapContentItem(it) }
}

fun List<MapRecord>.toRecordItems(): List<MapItem> {
    return this.map { MapRecordItem(it) }
}

/**
 * 혼합 리스트 생성 유틸리티
 */
fun createMixedMapItems(contents: List<MapContent>, records: List<MapRecord>): List<MapItem> {
    val contentItems = contents.toMapItems()
    val recordItems = records.toRecordItems()
    return (contentItems + recordItems).sortedByCreatedAt()
}