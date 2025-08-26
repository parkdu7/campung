package com.shinhan.campung.data.repository

import com.shinhan.campung.data.model.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMapContentRepository @Inject constructor() : MapContentRepository {
    
    // 테스트용 샘플 데이터
    private val sampleContents = mapOf(
        1L to MapContent(
            contentId = 1L,
            userId = "user1",
            author = Author("이벤트팀", false),
            location = Location(37.5665, 126.9780),
            postType = "PROMOTION",
            postTypeName = "홍보",
            markerType = "hot",
            contentScope = "MAP",
            contentType = "TEXT",
            title = "신한은행 본점 앞 이벤트",
            body = "신한은행 본점에서 진행하는 특별 이벤트입니다. 많은 참여 부탁드립니다!",
            mediaFiles = null,
            emotionTag = "SMILE",
            reactions = Reactions(12, 5),
            createdAt = LocalDateTime.now().minusHours(2).toString(),
            expiresAt = null
        ),
        2L to MapContent(
            contentId = 2L,
            userId = "user2",
            author = Author("맛집탐방러", false),
            location = Location(37.5665, 126.9780),
            postType = "FREE",
            postTypeName = "자유",
            markerType = "free",
            contentScope = "MAP",
            contentType = "TEXT",
            title = "점심 맛집 추천",
            body = "을지로 맛집 추천합니다. 가성비 좋고 맛있어요~",
            mediaFiles = null,
            emotionTag = "SMILE",
            reactions = Reactions(8, 3),
            createdAt = LocalDateTime.now().minusHours(1).toString(),
            expiresAt = null
        ),
        3L to MapContent(
            contentId = 3L,
            userId = "user3",
            author = Author("노트북판매자", false),
            location = Location(37.5640, 126.9750),
            postType = "MARKET",
            postTypeName = "장터",
            markerType = "market",
            contentScope = "MAP",
            contentType = "TEXT",
            title = "중고 노트북 판매",
            body = "맥북 프로 16인치 판매합니다. 상태 양호하며 박스 포함입니다.",
            mediaFiles = null,
            emotionTag = "SMILE",
            reactions = Reactions(15, 8),
            createdAt = LocalDateTime.now().minusMinutes(30).toString(),
            expiresAt = null
        ),
        4L to MapContent(
            contentId = 4L,
            userId = "user4",
            author = Author("종로토박이", false),
            location = Location(37.5660, 126.9820),
            postType = "INFO",
            postTypeName = "정보",
            markerType = "hot",
            contentScope = "MAP",
            contentType = "TEXT",
            title = "종로 맛집 정보",
            body = "종로3가 근처 숨겨진 맛집을 소개합니다. 현지인만 아는 그 곳!",
            mediaFiles = null,
            emotionTag = "SMILE",
            reactions = Reactions(25, 12),
            createdAt = LocalDateTime.now().minusMinutes(15).toString(),
            expiresAt = null
        ),
        5L to MapContent(
            contentId = 5L,
            userId = "user5",
            author = Author("카페사장", false),
            location = Location(37.5660, 126.9820),
            postType = "INFO",
            postTypeName = "정보",
            markerType = "info",
            contentScope = "MAP",
            contentType = "TEXT",
            title = "카페 알바 구합니다",
            body = "종로 카페에서 파트타임 직원을 구합니다. 시급 협의 가능합니다.",
            mediaFiles = null,
            emotionTag = "SMILE",
            reactions = Reactions(6, 2),
            createdAt = LocalDateTime.now().minusMinutes(45).toString(),
            expiresAt = null
        ),
        6L to MapContent(
            contentId = 6L,
            userId = "user6",
            author = Author("스터디장", false),
            location = Location(37.5660, 126.9820),
            postType = "FREE",
            postTypeName = "자유",
            markerType = "free",
            contentScope = "MAP",
            contentType = "TEXT",
            title = "스터디 모집",
            body = "토익 스터디원을 모집합니다. 주 3회 만나서 공부해요~",
            mediaFiles = null,
            emotionTag = "SMILE",
            reactions = Reactions(9, 4),
            createdAt = LocalDateTime.now().minusHours(3).toString(),
            expiresAt = null
        )
    )
    
    override suspend fun getContentById(contentId: Long): Result<MapContent> {
        // API 호출 시뮬레이션을 위한 딜레이
        delay(500)
        
        return sampleContents[contentId]?.let {
            Result.success(it)
        } ?: Result.failure(Exception("Content not found: $contentId"))
    }
    
    override suspend fun getContentsByIds(contentIds: List<Long>): Result<List<MapContent>> {
        // API 호출 시뮬레이션을 위한 딜레이
        delay(800)
        
        return try {
            val contents = contentIds.mapNotNull { id ->
                sampleContents[id]
            }
            Result.success(contents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}