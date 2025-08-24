package com.example.campung.content.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ThumbnailService {
    
    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;
    private static final String THUMBNAIL_FORMAT = "jpg";
    
    public byte[] generateImageThumbnail(MultipartFile file) throws IOException {
        if (!isImage(file)) {
            throw new IllegalArgumentException("이미지 파일만 썸네일 생성 가능합니다");
        }
        
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("이미지를 읽을 수 없습니다");
        }
        
        BufferedImage thumbnailImage = createThumbnail(originalImage);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnailImage, THUMBNAIL_FORMAT, baos);
        
        return baos.toByteArray();
    }
    
    public InputStream generateImageThumbnailAsStream(MultipartFile file) throws IOException {
        byte[] thumbnailBytes = generateImageThumbnail(file);
        return new ByteArrayInputStream(thumbnailBytes);
    }
    
    public String generateThumbnailFileName(String originalFileName) {
        String nameWithoutExtension = originalFileName;
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
        }
        return nameWithoutExtension + "_thumb." + THUMBNAIL_FORMAT;
    }
    
    private BufferedImage createThumbnail(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        // 비율을 유지하면서 썸네일 크기 계산
        double scale = Math.min(
            (double) THUMBNAIL_WIDTH / originalWidth,
            (double) THUMBNAIL_HEIGHT / originalHeight
        );
        
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);
        
        BufferedImage thumbnail = new BufferedImage(
            THUMBNAIL_WIDTH, 
            THUMBNAIL_HEIGHT, 
            BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = thumbnail.createGraphics();
        
        // 고품질 렌더링 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 배경을 흰색으로 채우기
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        // 이미지를 중앙에 배치
        int x = (THUMBNAIL_WIDTH - scaledWidth) / 2;
        int y = (THUMBNAIL_HEIGHT - scaledHeight) / 2;
        
        g2d.drawImage(original, x, y, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return thumbnail;
    }
    
    public boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
    
    public boolean canGenerateThumbnail(MultipartFile file) {
        return isImage(file);
    }
}