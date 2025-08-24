package com.example.campung.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {
    
    @Value("${firebase.config.path:firebase-service-account.json}")
    private String firebaseConfigPath;
    
    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
                
                if (resource.exists()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                            .build();
                    
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase application initialized successfully");
                } else {
                    log.warn("Firebase config file not found at: {}. FCM features will be disabled.", firebaseConfigPath);
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }
}