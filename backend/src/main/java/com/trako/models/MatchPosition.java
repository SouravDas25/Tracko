package com.trako.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchPosition {
    private int start;
    private int end;
    private double similarity;
    private String matchedText;
}
