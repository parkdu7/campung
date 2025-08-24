package com.example.Campung.Comment.Service;

import com.example.Campung.Comment.Dto.CommentDto;
import com.example.Campung.Comment.Dto.CommentListResponse;
import com.example.Campung.Entity.Comment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {
    
    public CommentListResponse toCommentListResponse(List<Comment> comments) {
        List<CommentDto> commentDtos = comments.stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());
        
        CommentListResponse.CommentData data = new CommentListResponse.CommentData(commentDtos);
        return new CommentListResponse(true, "댓글 조회 성공", data);
    }
    
    public CommentDto toCommentDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setCommentId(comment.getCommentId());
        dto.setUserId(comment.getAuthor().getUserId());
        dto.setBody(comment.getCommentContent());
        dto.setCreatedAt(comment.getCreatedAt());
        
        dto.setAuthor(toAuthorDto(comment));
        dto.setMediaFiles(List.of()); // TODO: 미디어 파일 처리 추후 구현
        dto.setReplies(toReplyDtos(comment.getReplies()));
        
        return dto;
    }
    
    private CommentDto.AuthorDto toAuthorDto(Comment comment) {
        CommentDto.AuthorDto authorDto = new CommentDto.AuthorDto();
        authorDto.setNickname(comment.getAuthor().getNickname());
        authorDto.setProfileImageUrl(null); // TODO: User 엔티티에 profileImageUrl 필드 추가 필요
        authorDto.setAnonymous(comment.getIsAnonymous());
        return authorDto;
    }
    
    private List<CommentDto.ReplyDto> toReplyDtos(List<Comment> replies) {
        if (replies == null || replies.isEmpty()) {
            return List.of();
        }
        
        return replies.stream()
                .map(this::toReplyDto)
                .collect(Collectors.toList());
    }
    
    private CommentDto.ReplyDto toReplyDto(Comment reply) {
        CommentDto.ReplyDto replyDto = new CommentDto.ReplyDto();
        replyDto.setReplyId(reply.getCommentId());
        replyDto.setUserId(reply.getAuthor().getUserId());
        replyDto.setBody(reply.getCommentContent());
        replyDto.setCreatedAt(reply.getCreatedAt());
        
        CommentDto.AuthorDto authorDto = new CommentDto.AuthorDto();
        authorDto.setNickname(reply.getAuthor().getNickname());
        authorDto.setProfileImageUrl(null); // TODO: User 엔티티에 profileImageUrl 필드 추가 필요
        authorDto.setAnonymous(reply.getIsAnonymous());
        replyDto.setAuthor(authorDto);
        
        replyDto.setMediaFiles(List.of()); // TODO: 미디어 파일 처리 추후 구현
        
        return replyDto;
    }
}