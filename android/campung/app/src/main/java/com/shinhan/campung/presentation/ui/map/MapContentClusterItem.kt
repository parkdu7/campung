package com.shinhan.campung.presentation.ui.map

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusteringKey
import com.shinhan.campung.data.model.MapContent

data class MapContentClusterItem(
    val mapContent: MapContent
) : ClusteringKey {
    
    override fun getPosition(): LatLng {
        return LatLng(mapContent.location.latitude, mapContent.location.longitude)
    }
    
    fun getTitle(): String = mapContent.title
    fun getSnippet(): String = mapContent.body ?: ""
    fun getPostType(): String = mapContent.postType
    fun getMarkerType(): String = mapContent.markerType
}