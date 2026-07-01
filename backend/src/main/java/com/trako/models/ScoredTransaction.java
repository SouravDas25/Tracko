package com.trako.models;

import com.trako.entities.Category;
import com.trako.entities.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoredTransaction {
    private Transaction transaction;
    private Category category;
    private double relevanceScore;
    private List<String> matchedFields;
    private Map<String, List<MatchPosition>> matchPositions;
}
