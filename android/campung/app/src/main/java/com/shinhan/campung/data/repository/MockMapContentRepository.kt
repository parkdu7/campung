package com.shinhan.campung.data.repository

import com.shinhan.campung.data.model.ContentCategory
import com.shinhan.campung.data.model.MapContent
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
            title = "신한은행 본점 앞 이벤트",
            content = "신한은행 본점에서 진행하는 특별 이벤트입니다. 많은 참여 부탁드립니다!",
            authorNickname = "이벤트팀",
            category = ContentCategory.PROMOTION,
            thumbnailUrl = null,
            latitude = 37.5665,
            longitude = 126.9780,
            likeCount = 12,
            commentCount = 5,
            createdAt = LocalDateTime.now().minusHours(2),
            isHot = true
        ),
        2L to MapContent(
            contentId = 2L,
            title = "점심 맛집 추천",
            content = "을지로 맛집 추천합니다. 가성비 좋고 맛있어요~",
            authorNickname = "맛집탐방러",
            category = ContentCategory.FREE,
            thumbnailUrl = null,
            latitude = 37.5665,
            longitude = 126.9780,
            likeCount = 8,
            commentCount = 3,
            createdAt = LocalDateTime.now().minusHours(1),
            isHot = false
        ),
        3L to MapContent(
            contentId = 3L,
            title = "중고 노트북 판매",
            content = "맥북 프로 16인치 판매합니다. 상태 양호하며 박스 포함입니다.",
            authorNickname = "노트북판매자",
            category = ContentCategory.MARKET,
            thumbnailUrl = null,
            latitude = 37.5640,
            longitude = 126.9750,
            likeCount = 15,
            commentCount = 8,
            createdAt = LocalDateTime.now().minusMinutes(30),
            isHot = false
        ),
        4L to MapContent(
            contentId = 4L,
            title = "종로 맛집 정보",
            content = "종로3가 근처 숨겨진 맛집을 소개합니다. 현지인만 아는 그 곳!",
            authorNickname = "종로토박이",
            category = ContentCategory.INFO,
            thumbnailUrl = null,
            latitude = 37.5660,
            longitude = 126.9820,
            likeCount = 25,
            commentCount = 12,
            createdAt = LocalDateTime.now().minusMinutes(15),
            isHot = true
        ),
        5L to MapContent(
            contentId = 5L,
            title = "카페 알바 구합니다",
            content = "종로 카페에서 파트타임 직원을 구합니다. 시급 협의 가능합니다.",
            authorNickname = "카페사장",
            category = ContentCategory.INFO,
            thumbnailUrl = null,
            latitude = 37.5660,
            longitude = 126.9820,
            likeCount = 6,
            commentCount = 2,
            createdAt = LocalDateTime.now().minusMinutes(45),
            isHot = false
        ),
        6L to MapContent(
            contentId = 6L,
            title = "스터디 모집",
            content = "토익 스터디원을 모집합니다. 주 3회 만나서 공부해요~",
            authorNickname = "스터디장",
            category = ContentCategory.FREE,
            thumbnailUrl = null,
            latitude = 37.5660,
            longitude = 126.9820,
            likeCount = 9,
            commentCount = 4,
            createdAt = LocalDateTime.now().minusHours(3),
            isHot = false
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