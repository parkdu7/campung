package com.example.Campung.Content.dto;

import com.example.Campung.Global.Enum.PostType;

public class ContentSearchRequest {
    private String q;
    private PostType postType;
    private Integer page = 1;
    private Integer size = 20;
    
    public String getQ() {
        return q;
    }
    
    public void setQ(String q) {
        this.q = q;
    }
    
    public PostType getPostType() {
        return postType;
    }
    
    public void setPostType(PostType postType) {
        this.postType = postType;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
}