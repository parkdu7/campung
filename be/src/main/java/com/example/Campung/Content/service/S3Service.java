package com.example.Campung.Content.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class S3Service {
    
    @Value("${AWS_ACCESS_KEY_ID}")
    private String accessKeyId;
    
    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretAccessKey;
    
    @Value("${AWS_REGION}")
    private String region;
    
    @Value("${S3_BUCKET_NAME}")
    private String bucketName;
    
    private S3Client getS3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }
    
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }
        
        String contentType = file.getContentType();
        String folder = determineFolder(contentType);
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folder + "/contents/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + fileName;
        
        try (S3Client s3Client = getS3Client()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }
        
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
    
    private String determineFolder(String contentType) {
        if (contentType == null) {
            return "audios";
        }
        
        if (contentType.startsWith("image/")) {
            return "images";
        } else if (contentType.startsWith("video/")) {
            return "videos";
        } else if (contentType.startsWith("audio/")) {
            return "audios";
        } else {
            return "audios";
        }
    }
    
    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}