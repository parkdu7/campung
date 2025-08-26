package com.shinhan.campung.util

import ch.hsr.geohash.GeoHash

object GeohashUtil {
    private const val PRECISION = 8 // 8자리 Geohash (약 38m x 19m)
    
    fun encode(latitude: Double, longitude: Double): String {
        return GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, PRECISION)
    }
    
    fun isSignificantMove(oldGeohash: String?, newGeohash: String): Boolean {
        return oldGeohash != newGeohash
    }
    
    fun isValidGeohash(geohash: String): Boolean {
        return geohash.matches(Regex("^[0-9b-hjkmnp-z]{8}$"))
    }
}