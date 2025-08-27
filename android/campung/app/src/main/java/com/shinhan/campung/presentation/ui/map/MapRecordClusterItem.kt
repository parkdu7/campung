package com.shinhan.campung.presentation.ui.map

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusteringKey
import com.shinhan.campung.data.model.MapRecord

data class MapRecordClusterItem(
    val mapRecord: MapRecord
) : ClusteringKey {
    
    override fun getPosition(): LatLng {
        return LatLng(mapRecord.location.latitude, mapRecord.location.longitude)
    }
    
    fun getRecordUrl(): String = mapRecord.recordUrl
    fun getMarkerType(): String = mapRecord.markerType
    fun getUserId(): String = mapRecord.userId
}