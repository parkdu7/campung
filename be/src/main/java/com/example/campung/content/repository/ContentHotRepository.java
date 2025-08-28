package com.example.campung.content.repository;

import com.example.campung.entity.ContentHot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentHotRepository extends JpaRepository<ContentHot, Long> {
    
    List<ContentHot> findTop10ByOrderByHotScoreDesc();
    
    boolean existsByContentId(Long contentId);
    
    void deleteByContentId(Long contentId);
    
    Optional<ContentHot> findByContentId(Long contentId);
}