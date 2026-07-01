package com.trako.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TransactionSearchHitDTO {
    private TransactionDetailDTO transaction;
    private Double relevanceScore;
    private Map<String, String> highlights;
    private List<String> matchedFields;
}
