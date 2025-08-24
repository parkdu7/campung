package com.example.Campung.Comment.Service;

import com.example.Campung.Comment.Dto.CommentListResponse;
import com.example.Campung.Comment.Repository.CommentRepository;
import com.example.Campung.Content.Repository.ContentRepository;
import com.example.Campung.Global.Exception.ContentNotFoundException;
import com.example.Campung.Entity.Comment;
import com.example.Campung.Entity.Content;
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