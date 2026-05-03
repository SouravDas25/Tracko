package com.trako.models;

import com.trako.entities.Category;
import com.trako.entities.Transaction;

import java.util.List;
import java.util.Map;

public class ScoredTransaction {

    private Transaction transaction;
    private Category category;
    private double relevanceScore;
    private List<String> matchedFields;
    private Map<String, List<MatchPosition>> matchPositions;

    public ScoredTransaction() {
    }

    public ScoredTransaction(Transaction transaction, Category category, double relevanceScore,
                             List<String> matchedFields, Map<String, List<MatchPosition>> matchPositions) {
        this.transaction = transaction;
        this.category = category;
        this.relevanceScore = relevanceScore;
        this.matchedFields = matchedFields;
        this.matchPositions = matchPositions;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }

    public Map<String, List<MatchPosition>> getMatchPositions() {
        return matchPositions;
    }

    public void setMatchPositions(Map<String, List<MatchPosition>> matchPositions) {
        this.matchPositions = matchPositions;
    }
}
