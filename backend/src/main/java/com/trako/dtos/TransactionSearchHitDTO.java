package com.trako.dtos;

import java.util.List;
import java.util.Map;

public class TransactionSearchHitDTO {
    private TransactionDetailDTO transaction;
    private Double relevanceScore;
    private Map<String, String> highlights; // field -> highlighted text
    private List<String> matchedFields;

    public TransactionDetailDTO getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionDetailDTO transaction) {
        this.transaction = transaction;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Map<String, String> getHighlights() {
        return highlights;
    }

    public void setHighlights(Map<String, String> highlights) {
        this.highlights = highlights;
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }
}