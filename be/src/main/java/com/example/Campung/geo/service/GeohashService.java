package com.example.Campung.geo.service;

import com.github.davidmoten.geo.GeoHash;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class GeohashService {
    
    public String geohash8(double lat, double lon) {
        return GeoHash.encodeHash(lat, lon, 8);
    }
    
    public Set<String> neighbors3x3(String hash8) {
        var set = new LinkedHashSet<String>();
        set.add(hash8);
        set.addAll(GeoHash.neighbours(hash8));
        return set;
    }
}