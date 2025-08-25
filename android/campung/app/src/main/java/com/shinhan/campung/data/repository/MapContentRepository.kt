package com.shinhan.campung.data.repository

import com.shinhan.campung.data.mapper.ContentMapper
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.remote.api.MapApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface MapContentRepository {
    suspend fun getContentById(contentId: Long): Result<MapContent>
    suspend fun getContentsByIds(contentIds: List<Long>): Result<List<MapContent>>
}

@Singleton
class MapContentRepositoryImpl @Inject constructor(
    private val apiService: MapApiService,
    private val mapper: ContentMapper
) : MapContentRepository {
    
    override suspend fun getContentById(contentId: Long): Result<MapContent> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getContent(contentId)
                if (response.success) {
                    Result.success(mapper.toMapContent(response.data))
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    override suspend fun getContentsByIds(contentIds: List<Long>): Result<List<MapContent>> =
        withContext(Dispatchers.IO) {
            try {
                val contents = contentIds.mapNotNull { id ->
                    getContentById(id).getOrNull()
                }
                Result.success(contents)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}