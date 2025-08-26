package com.example.campung.notification.service;

import com.example.campung.geo.service.GeohashService;
import com.example.campung.notification.dto.NewPostEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostEventPublisher {
    
    private final GeohashService geohash;
    private final SimpMessagingTemplate broker;
    
    public void publishNewPost(long postId, double lat, double lon) {
        String cell = geohash.geohash8(lat, lon);
        var event = new NewPostEvent(postId, lat, lon, System.currentTimeMillis());
        
        try {
            // 경계 보강: 중심 셀 + 8개 이웃 셀에 발행 (약 100m 반경)
            for (String neighborCell : geohash.neighbors3x3(cell)) {
                String topic = "/topic/newpost/" + neighborCell;
                broker.convertAndSend(topic, event);
                log.debug("Published to topic: {}", topic);
            }
            log.info("Published new post event to 9 cells: postId={}, centerCell={}", postId, cell);
        } catch (Exception e) {
            log.error("Failed to publish new post event: postId={}, centerCell={}", postId, cell, e);
        }
    }
}