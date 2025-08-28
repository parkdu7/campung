package com.shinhan.campung.data.repository

import android.util.Log
import com.shinhan.campung.data.mapper.ContentMapper
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.remote.api.MapApiService
import com.shinhan.campung.data.remote.api.ContentsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface MapContentRepository {
    suspend fun getContentById(contentId: Long): Result<MapContent>
    suspend fun getContentsByIds(contentIds: List<Long>): Result<List<MapContent>>
    suspend fun getHotContents(): Result<List<MapContent>>
}

@Singleton
class MapContentRepositoryImpl @Inject constructor(
    private val apiService: MapApiService,
    private val contentsApiService: ContentsApiService,
    private val mapper: ContentMapper
) : MapContentRepository {
    
    override suspend fun getContentById(contentId: Long): Result<MapContent> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d("MapContentRepository", "📡 [FLOW] API 호출 시작 - contentId: $contentId")
                val response = apiService.getContent(contentId)
                Log.d("MapContentRepository", "📡 [FLOW] API 응답 - success: ${response.success}, message: ${response.message}")
                if (response.success) {
                    Log.d("MapContentRepository", "✅ [FLOW] API 성공 - 원본 데이터:")
                    Log.d("MapContentRepository", "  - reactions: ${response.data.reactions}")
                    Log.d("MapContentRepository", "  - likeCount: ${response.data.reactions?.likes ?: 0}")
                    Log.d("MapContentRepository", "  - commentCount: ${response.data.reactions?.comments ?: 0}")  
                    Log.d("MapContentRepository", "  - createdAt: ${response.data.createdAt}")
                    Log.d("MapContentRepository", "  - author.nickname: ${response.data.author.nickname}")
                    Log.d("MapContentRepository", "🔄 [FLOW] 데이터 매핑 중")
                    Result.success(mapper.toMapContent(response.data))
                } else {
                    Log.w("MapContentRepository", "⚠️ [FLOW] API 응답 실패 - message: ${response.message}")
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Log.e("MapContentRepository", "❌ [FLOW] API 호출 예외", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getContentsByIds(contentIds: List<Long>): Result<List<MapContent>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("MapContentRepository", "🚀 [FLOW] getContentsByIds 시작 - ID들: $contentIds")
                val contents = contentIds.mapNotNull { id ->
                    Log.d("MapContentRepository", "📡 [FLOW] 개별 컨텐츠 요청 - ID: $id")
                    val result = getContentById(id)
                    if (result.isSuccess) {
                        Log.d("MapContentRepository", "✅ [FLOW] 개별 컨텐츠 로딩 성공 - ID: $id")
                        result.getOrNull()
                    } else {
                        Log.w("MapContentRepository", "❌ [FLOW] 개별 컨텐츠 로딩 실패 - ID: $id, 에러: ${result.exceptionOrNull()?.message}")
                        null
                    }
                }
                Log.d("MapContentRepository", "✅ [FLOW] getContentsByIds 완료 - 성공한 컨텐츠: ${contents.size}개")
                Result.success(contents)
            } catch (e: Exception) {
                Log.e("MapContentRepository", "❌ [FLOW] getContentsByIds 예외 발생", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getHotContents(): Result<List<MapContent>> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d("MapContentRepository", "🔥 [FLOW] getHotContents 시작 - 핫 게시글 조회")
                val response = contentsApiService.getHotContents()
                
                Log.d("MapContentRepository", "📡 [FLOW] 핫 게시글 API 응답 - code: ${response.code()}, successful: ${response.isSuccessful}")
                Log.d("MapContentRepository", "📡 [FLOW] 응답 body - success: ${response.body()?.success}, message: ${response.body()?.message}")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val hotContentsData = response.body()?.data
                    
                    if (hotContentsData != null) {
                        Log.d("MapContentRepository", "✅ [FLOW] 핫 게시글 API 성공 - ${hotContentsData.size}개 조회")
                        
                        val hotContents = hotContentsData.map { contentData ->
                            Log.d("MapContentRepository", "🔄 [FLOW] 핫 게시글 매핑 - ID: ${contentData.contentId}, 제목: ${contentData.title}")
                            mapper.toMapContent(contentData)
                        }
                        
                        Log.d("MapContentRepository", "✅ [FLOW] getHotContents 완료 - 매핑된 핫 게시글: ${hotContents.size}개")
                        Result.success(hotContents)
                    } else {
                        Log.w("MapContentRepository", "⚠️ [FLOW] 핫 게시글 데이터가 null - 빈 리스트 반환")
                        Result.success(emptyList())
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "핫 게시글 조회 실패"
                    Log.w("MapContentRepository", "⚠️ [FLOW] 핫 게시글 API 응답 실패 - $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("MapContentRepository", "❌ [FLOW] getHotContents 예외 발생", e)
                Result.failure(e)
            }
        }
}