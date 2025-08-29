package com.shinhan.campung.data.service.fcm

import com.google.firebase.messaging.RemoteMessage

/**
 * FCM 알림 처리를 위한 인터페이스
 * 각 알림 타입별로 별도의 Handler 구현체를 만들어 단일 책임 원칙을 따름
 */
interface NotificationHandler {
    /**
     * 이 Handler가 처리할 수 있는 알림 타입인지 확인
     * @param type FCM 메시지의 type 필드
     * @return 처리 가능하면 true
     */
    fun canHandle(type: String?): Boolean
    
    /**
     * 알림을 처리 (알림 생성 및 표시)
     * @param data FCM 메시지의 data 필드
     * @param notification FCM 메시지의 notification 필드 (nullable)
     */
    fun handle(data: Map<String, String>, notification: RemoteMessage.Notification?)
}