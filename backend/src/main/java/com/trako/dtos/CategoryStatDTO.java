package com.trako.dtos;

public class CategoryStatDTO {
    private Long categoryId;
    private String categoryName;
    private Double amount;

    public CategoryStatDTO() {
    }

    public CategoryStatDTO(Long categoryId, String categoryName, Double amount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
