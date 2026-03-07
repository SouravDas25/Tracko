package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionEntryTypeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.util.Date;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "transaction_type")
    @Convert(converter = TransactionEntryTypeConverter.class)
    private TransactionDbType transactionType;

    @NotNull
    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "comments", length = 512)
    private String comments;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date")
    private Date date;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "amount", insertable = false, updatable = false)
    private Double amount;

    @NotNull
    @Column(name = "original_currency", length = 3)
    private String originalCurrency;

    @NotNull
    @Column(name = "original_amount")
    private Double originalAmount;

    @NotNull
    @Column(name = "exchange_rate")
    private Double exchangeRate;

    @NotNull
    @Column(name = "account_id")
    private Long accountId;

    @NotNull
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "is_countable")
    private Integer isCountable = 1;

    @Column(name = "linked_transaction_id")
    private Long linkedTransactionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @Transient
    private Integer renderedTransactionType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public TransactionDbType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionDbType transactionType) {
        this.transactionType = transactionType;
    }

    @JsonProperty("transactionType")
    public Integer getRenderedTransactionType() {
        return renderedTransactionType != null ? renderedTransactionType : (transactionType != null ? transactionType.getValue() : null);
    }

    public void setRenderedTransactionType(Integer renderedTransactionType) {
        this.renderedTransactionType = renderedTransactionType;
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

    public Long getLinkedTransactionId() {
        return linkedTransactionId;
    }

    public void setLinkedTransactionId(Long linkedTransactionId) {
        this.linkedTransactionId = linkedTransactionId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
