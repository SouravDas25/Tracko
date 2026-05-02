package com.trako.models;

public class MatchPosition {

    private int start;
    private int end;
    private double similarity;
    private String matchedText;

    public MatchPosition() {
    }

    public MatchPosition(int start, int end, double similarity, String matchedText) {
        this.start = start;
        this.end = end;
        this.similarity = similarity;
        this.matchedText = matchedText;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }
}
