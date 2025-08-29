package com.example.campung.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
@Slf4j
public class DefaultImageUploader {
    
    @Value("${AWS_ACCESS_KEY_ID}")
    private String accessKeyId;
    
    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretAccessKey;
    
    @Value("${AWS_REGION}")
    private String region;
    
    @Value("${S3_BUCKET_NAME}")
    private String bucketName;
    
    @EventListener(ApplicationReadyEvent.class)
    public void uploadDefaultImageIfNotExists() {
        String key = "images/profiles/default/defaultImage.png";
        
        try {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
            
            // S3에 파일이 이미 있는지 확인
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
                log.info("Default profile image already exists in S3");
                return;
            } catch (NoSuchKeyException e) {
                // 파일이 없으므로 업로드 진행
                log.info("Default profile image not found in S3, uploading...");
            }
            
            ClassPathResource defaultImage = new ClassPathResource("defaultImage.png");
            
            if (!defaultImage.exists()) {
                log.warn("defaultImage.png not found in resources folder");
                return;
            }
            
            try (InputStream inputStream = defaultImage.getInputStream()) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("image/png")
                        .build();
                
                s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(inputStream, defaultImage.contentLength()));
                
                String url = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
                log.info("Default profile image uploaded successfully: {}", url);
            }
            
        } catch (Exception e) {
            log.error("Failed to upload default profile image", e);
        }
    }
}