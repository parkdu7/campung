package com.example.Campung.Content.repository;

import com.example.Campung.entity.Content;
import com.example.Campung.Global.Enum.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    @Query("SELECT c FROM Content c WHERE " +
           "(:query IS NULL OR c.title LIKE %:query% OR c.content LIKE %:query%) AND " +
           "(:postType IS NULL OR c.postType = :postType)")
    Page<Content> searchContents(@Param("query") String query, 
                               @Param("postType") PostType postType, 
                               Pageable pageable);
    
    @Query("SELECT c FROM Content c WHERE " +
           "(:postType IS NULL OR c.postType = :postType) " +
           "ORDER BY c.createdAt DESC")
    List<Content> findContentsByPostType(@Param("postType") PostType postType);
}