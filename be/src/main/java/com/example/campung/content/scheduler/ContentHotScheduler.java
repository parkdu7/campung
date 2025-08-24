package com.example.campung.content.scheduler;

import com.example.campung.content.service.ContentHotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContentHotScheduler {
    
    @Autowired
    private ContentHotService contentHotService;
    
    // 매 30분마다 HOT 컨텐츠 업데이트
    @Scheduled(cron = "0 */30 * * * *")
    public void updateHotContent() {
        try {
            contentHotService.updateHotContent();
            System.out.println("HOT 컨텐츠 업데이트 완료: " + java.time.LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("HOT 컨텐츠 업데이트 실패: " + e.getMessage());
        }
    }
}