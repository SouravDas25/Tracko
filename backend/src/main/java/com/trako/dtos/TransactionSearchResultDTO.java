package com.trako.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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
}
