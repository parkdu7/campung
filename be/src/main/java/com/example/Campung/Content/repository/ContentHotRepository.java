package com.example.Campung.Content.Repository;

import com.example.Campung.Entity.ContentHot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentHotRepository extends JpaRepository<ContentHot, Long> {
    
    List<ContentHot> findTop10ByOrderByHotScoreDesc();
    
    boolean existsByContentId(Long contentId);
    
    void deleteByContentId(Long contentId);
}