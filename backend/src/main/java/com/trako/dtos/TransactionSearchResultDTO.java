package com.trako.dtos;

import java.util.List;

public class TransactionSearchResultDTO {
    private List<TransactionSearchHitDTO> results;
    private Long totalResults;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Long searchTimeMs;
    private String query;

    public List<TransactionSearchHitDTO> getResults() {
        return results;
    }

    public void setResults(List<TransactionSearchHitDTO> results) {
        this.results = results;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
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

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public Long getSearchTimeMs() {
        return searchTimeMs;
    }

    public void setSearchTimeMs(Long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}