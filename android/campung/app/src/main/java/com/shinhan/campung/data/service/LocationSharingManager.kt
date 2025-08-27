package com.shinhan.campung.data.service

import android.util.Log
import com.shinhan.campung.data.model.SharedLocation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 공유 기능을 관리하는 서비스
 */
@Singleton
class LocationSharingManager @Inject constructor() {
    
    companion object {
        private const val TAG = "LocationSharingManager"
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 공유된 위치들 관리
    private val _sharedLocations = MutableStateFlow<List<SharedLocation>>(emptyList())
    val sharedLocations: StateFlow<List<SharedLocation>> = _sharedLocations.asStateFlow()
    
    /**
     * 새로운 위치 공유 추가
     * @param userName 사용자 이름
     * @param latitude 위도
     * @param longitude 경도
     * @param displayUntilString UTC 시간 문자열
     * @param shareId 공유 ID
     */
    fun addSharedLocation(
        userName: String,
        latitude: Double,
        longitude: Double,
        displayUntilString: String,
        shareId: String
    ) {
        Log.d(TAG, "addSharedLocation 호출됨 - userName: $userName, shareId: $shareId")
        Log.d(TAG, "좌표: lat=$latitude, lng=$longitude")
        Log.d(TAG, "displayUntilString: $displayUntilString")
        
        try {
            // displayUntil을 한국 시간으로 변환 (+9시간)
            Log.d(TAG, "시간 파싱 시작: $displayUntilString")
            val utcDateTime = LocalDateTime.parse(displayUntilString)
            val koreanDateTime = utcDateTime.plusHours(9)
            Log.d(TAG, "시간 파싱 완료 - UTC: $utcDateTime, KST: $koreanDateTime")
            
            val sharedLocation = SharedLocation(
                userName = userName,
                latitude = latitude,
                longitude = longitude,
                displayUntil = koreanDateTime,
                shareId = shareId
            )
            Log.d(TAG, "SharedLocation 객체 생성 완료: $sharedLocation")
            
            val currentLocations = _sharedLocations.value.toMutableList()
            Log.d(TAG, "현재 위치 목록 크기: ${currentLocations.size}")
            
            // 같은 shareId가 있으면 업데이트, 없으면 추가
            val existingIndex = currentLocations.indexOfFirst { it.shareId == shareId }
            if (existingIndex != -1) {
                Log.d(TAG, "기존 위치 업데이트: index=$existingIndex")
                currentLocations[existingIndex] = sharedLocation
            } else {
                Log.d(TAG, "새 위치 추가")
                currentLocations.add(sharedLocation)
            }
            
            _sharedLocations.value = currentLocations
            Log.d(TAG, "StateFlow 업데이트 완료 - 새 크기: ${currentLocations.size}")
            
            Log.d(TAG, "위치 공유 추가 완료: $userName (만료: $koreanDateTime)")
            
            // 만료 시간에 자동으로 제거하는 타이머 시작
            scheduleLocationRemoval(shareId, koreanDateTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "위치 공유 데이터 파싱 실패", e)
            Log.e(TAG, "실패한 데이터: displayUntilString='$displayUntilString'")
        }
    }
    
    /**
     * 지정된 시간 후 위치 공유 자동 제거 예약
     */
    private fun scheduleLocationRemoval(shareId: String, displayUntil: LocalDateTime) {
        coroutineScope.launch {
            val now = LocalDateTime.now()
            val delayMillis = java.time.Duration.between(now, displayUntil).toMillis()
            
            if (delayMillis > 0) {
                Log.d(TAG, "위치 공유 자동 제거 예약: ${delayMillis}ms 후")
                delay(delayMillis)
                removeSharedLocation(shareId)
            } else {
                // 이미 만료된 경우 즉시 제거
                removeSharedLocation(shareId)
            }
        }
    }
    
    /**
     * 위치 공유 제거
     */
    fun removeSharedLocation(shareId: String) {
        val currentLocations = _sharedLocations.value.toMutableList()
        val removed = currentLocations.removeAll { it.shareId == shareId }
        if (removed) {
            _sharedLocations.value = currentLocations
            Log.d(TAG, "위치 공유 제거됨: shareId=$shareId")
        }
    }
    
    /**
     * 만료된 위치들을 주기적으로 정리
     */
    fun cleanupExpiredLocations() {
        val now = LocalDateTime.now()
        val currentLocations = _sharedLocations.value
        val activeLocations = currentLocations.filter { it.displayUntil.isAfter(now) }
        
        if (activeLocations.size != currentLocations.size) {
            _sharedLocations.value = activeLocations
            Log.d(TAG, "만료된 위치 공유 정리됨: ${currentLocations.size - activeLocations.size}개 제거")
        }
    }
    
    /**
     * 모든 위치 공유 제거
     */
    fun clearAllSharedLocations() {
        _sharedLocations.value = emptyList()
        Log.d(TAG, "모든 위치 공유 제거됨")
    }
}