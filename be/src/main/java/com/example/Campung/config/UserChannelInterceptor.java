package com.example.Campung.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Pattern;

@Component
public class UserChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_PATTERN = Pattern.compile("^/topic/newpost/[0-9b-hj-km-np-z]{8}$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String userId = accessor.getFirstNativeHeader("userId");
                if (userId == null || userId.trim().isEmpty()) {
                    throw new RuntimeException("Missing userId header");
                }
                accessor.setUser(new UserPrincipal(userId));
            }
            
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                if (destination != null && !TOPIC_PATTERN.matcher(destination).matches()) {
                    throw new RuntimeException("Invalid topic pattern: " + destination);
                }
            }
        }
        
        return message;
    }

    public static class UserPrincipal implements Principal {
        private final String name;

        public UserPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}