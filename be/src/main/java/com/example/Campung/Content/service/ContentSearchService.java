package com.example.Campung.Content.Service;

import com.example.Campung.Content.Dto.ContentSearchRequest;
import com.example.Campung.Content.Dto.ContentSearchResponse;
import com.example.Campung.Content.Repository.ContentRepository;
import com.example.Campung.Entity.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentSearchService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    public ContentSearchResponse searchContents(ContentSearchRequest request) {
        System.out.println("=== CONTENT 검색 시작 ===");
        System.out.println("검색어: " + request.getQ());
        System.out.println("게시글 타입: " + request.getPostType());
        
        if (request.getQ() == null || request.getQ().trim().isEmpty()) {
            return new ContentSearchResponse(false, "검색어를 입력해주세요");
        }
        
        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            request.getSize(), 
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<Content> contentPage = contentRepository.searchContents(
            request.getQ().trim(), 
            request.getPostType(), 
            pageable
        );
        
        List<ContentSearchResponse.ContentItem> contentItems = contentPage.getContent()
            .stream()
            .map(this::convertToContentItem)
            .collect(Collectors.toList());
        
        ContentSearchResponse.PaginationInfo pagination = new ContentSearchResponse.PaginationInfo(
            request.getPage(),
            contentPage.getTotalPages(),
            (int) contentPage.getTotalElements()
        );
        
        ContentSearchResponse.SearchData searchData = new ContentSearchResponse.SearchData(
            request.getQ(),
            (int) contentPage.getTotalElements(),
            contentItems,
            pagination
        );
        
        System.out.println("검색 결과: " + contentPage.getTotalElements() + "건");
        
        return new ContentSearchResponse(true, "검색 결과 조회 성공", searchData);
    }
    
    private ContentSearchResponse.ContentItem convertToContentItem(Content content) {
        ContentSearchResponse.ContentItem item = new ContentSearchResponse.ContentItem();
        
        item.setContentId(content.getContentId());
        item.setPostType(content.getPostType().name());
        item.setTitle(content.getTitle());
        
        String highlight = highlightSearchTerm(content.getTitle(), "");
        item.setHighlight(highlight);
        
        ContentSearchResponse.AuthorInfo author = new ContentSearchResponse.AuthorInfo(
            content.getAuthor().getNickname(),
            false
        );
        item.setAuthor(author);
        
        if (content.getLatitude() != null && content.getLongitude() != null) {
            ContentSearchResponse.LocationInfo location = new ContentSearchResponse.LocationInfo(
                "위치 정보"
            );
            item.setLocation(location);
        }
        
        ContentSearchResponse.ReactionInfo reactions = new ContentSearchResponse.ReactionInfo(
            0,
            0
        );
        item.setReactions(reactions);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        item.setCreatedAt(content.getCreatedAt().format(formatter) + "Z");
        
        return item;
    }
    
    private String highlightSearchTerm(String text, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return text;
        }
        return text.replaceAll("(?i)" + searchTerm, "**" + searchTerm + "**");
    }
}