package com.example.Campung.Comment.Service;

import com.example.Campung.Comment.Dto.CommentCreateRequest;
import com.example.Campung.Comment.Dto.CommentCreateResponse;
import com.example.Campung.Comment.Repository.CommentRepository;
import com.example.Campung.Content.Repository.ContentRepository;
import com.example.Campung.Content.Service.S3Service;
import com.example.Campung.Global.Exception.ContentNotFoundException;
import com.example.Campung.User.Repository.UserRepository;
import com.example.Campung.Entity.Comment;
import com.example.Campung.Entity.Content;
import com.example.Campung.Entity.User;
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
}