package com.example.campung.comment.service;

import com.example.campung.comment.dto.CommentListResponse;
import com.example.campung.comment.repository.CommentRepository;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.global.exception.ContentNotFoundException;
import com.example.campung.entity.Comment;
import com.example.campung.entity.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentListService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByContentId(Long contentId) {
        validateContentExists(contentId);
        
        List<Comment> comments = commentRepository.findByContentIdWithReplies(contentId);
        
        return commentMapper.toCommentListResponse(comments);
    }
    
    private void validateContentExists(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }
}