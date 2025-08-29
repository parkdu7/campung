package com.example.campung.comment.service;

import com.example.campung.comment.dto.CommentCreateRequest;
import com.example.campung.comment.dto.CommentCreateResponse;
import com.example.campung.comment.repository.CommentRepository;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.content.service.S3Service;
import com.example.campung.global.exception.ContentNotFoundException;
import com.example.campung.user.repository.UserRepository;
import com.example.campung.notification.service.NotificationService;
import com.example.campung.entity.Comment;
import com.example.campung.entity.Content;
import com.example.campung.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private NotificationService notificationService;
    
    @Transactional
    public CommentCreateResponse createComment(Long contentId, CommentCreateRequest request, String accessToken) throws IOException {
        System.out.println("=== 댓글 작성 시작 ===");
        System.out.println("contentId: " + contentId);
        System.out.println("accessToken: " + accessToken);
        System.out.println("body: " + request.getBody());
        
        validateCommentRequest(request);
        System.out.println("유효성 검증 완료");
        
        // 게시글 존재 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        
        // 부모 댓글 확인 (대댓글인 경우)
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 댓글입니다"));
            
            // 부모 댓글이 같은 게시글에 속하는지 확인
            if (!parentComment.getContent().getContentId().equals(contentId)) {
                throw new IllegalArgumentException("부모 댓글이 해당 게시글에 속하지 않습니다");
            }
            System.out.println("부모 댓글 확인 완료: " + parentComment.getCommentId());
        }
        
        // 사용자 조회 또는 생성
        User author = userRepository.findByUserId(accessToken)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(accessToken)
                            .nickname(accessToken)
                            .passwordHash("temp_hash")
                            .build();
                    return userRepository.save(newUser);
                });
        System.out.println("User 조회/생성 완료: " + author.getUserId());
        
        // 댓글 생성
        Comment.CommentBuilder commentBuilder = Comment.builder()
                .content(content)
                .author(author)
                .commentContent(request.getBody())
                .isAnonymous(request.getIsAnonymous());
        
        // 대댓글인 경우 부모 댓글 설정
        if (parentComment != null) {
            commentBuilder.parentComment(parentComment);
        }
        
        Comment comment = commentBuilder.build();
        
        Comment savedComment = commentRepository.save(comment);
        System.out.println("=== 댓글 저장 완료 ===");
        System.out.println("저장된 댓글 ID: " + savedComment.getCommentId());
        
        // 댓글 알림 전송 (본인이 작성한 게시글이 아닌 경우에만)
        if (!content.getAuthor().getUserId().equals(accessToken)) {
            sendCommentNotification(content, author);
        }
        
        return new CommentCreateResponse(true, "댓글이 성공적으로 작성되었습니다", savedComment.getCommentId());
    }
    
    private void validateCommentRequest(CommentCreateRequest request) {
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요");
        }
        
        if (request.getIsAnonymous() == null) {
            throw new IllegalArgumentException("익명 여부를 설정해주세요");
        }
    }
    
    private void sendCommentNotification(Content content, User commenter) {
        try {
            User postAuthor = content.getAuthor();
            String commenterName = commenter.getUserId().equals("anonymous") || (commenter.getNickname() != null && commenter.getNickname().contains("익명")) 
                    ? "익명" 
                    : (commenter.getNickname() != null ? commenter.getNickname() : commenter.getUserId());
            
            String contentTitle = content.getTitle();
            if (contentTitle.length() > 10) {
                contentTitle = contentTitle.substring(0, 10) + "...";
            }
            
            String message = commenterName + " 님이 " + contentTitle + " 글에 댓글을 작성했습니다.";
            String title = "댓글 알림";
            String type = "normal";
            String data = "{\"contentId\":" + content.getContentId() + "}";
            
            notificationService.createNotification(postAuthor, type, title, message, data);
        } catch (Exception e) {
            System.err.println("댓글 알림 전송 중 오류 발생: " + e.getMessage());
        }
    }
}