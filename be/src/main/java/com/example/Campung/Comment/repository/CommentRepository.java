package com.example.Campung.Comment.repository;

import com.example.Campung.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.replies r LEFT JOIN FETCH r.author " +
           "WHERE c.content.contentId = :contentId AND c.parentComment IS NULL " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findByContentIdWithReplies(@Param("contentId") Long contentId);
}