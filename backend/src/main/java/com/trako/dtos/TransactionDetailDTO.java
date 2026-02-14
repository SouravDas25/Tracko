package com.trako.dtos;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;

import java.util.Date;
import java.util.List;

public class TransactionDetailDTO {
    private Long id;
    private Integer transactionType;
    private String name;
    private String comments;
    private Date date;
    private Double amount;
    private String originalCurrency;
    private Double originalAmount;
    private Double exchangeRate;
    private Long accountId;
    private Long categoryId;
    private Integer isCountable;

    private Category category;
    private Account account;
    private List<SplitDetailDTO> splits;

    public TransactionDetailDTO(Transaction transaction, Category category, Account account, List<SplitDetailDTO> splits) {
        this.id = transaction.getId();
        this.transactionType = transaction.getTransactionType();
        this.name = transaction.getName();
        this.comments = transaction.getComments();
        this.date = transaction.getDate();
        this.amount = transaction.getAmount();
        this.originalCurrency = transaction.getOriginalCurrency();
        this.originalAmount = transaction.getOriginalAmount();
        this.exchangeRate = transaction.getExchangeRate();
        this.accountId = transaction.getAccountId();
        this.categoryId = transaction.getCategoryId();
        this.isCountable = transaction.getIsCountable();
        this.category = category;
        this.account = account;
        this.splits = splits;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public Double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getIsCountable() {
        return isCountable;
    }

    public void setIsCountable(Integer isCountable) {
        this.isCountable = isCountable;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<SplitDetailDTO> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitDetailDTO> splits) {
        this.splits = splits;
    }
}
