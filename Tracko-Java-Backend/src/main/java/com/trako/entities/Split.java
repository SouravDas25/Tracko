package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(name = "splits")
public class Split extends AbstractBaseEntity {

    @NotNull
    @Column(name = "source_user_id")
    private String sourceUserId;

    @NotNull
    @Column(name = "due_user_id")
    private String dueUserId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_user_id", insertable = false, updatable = false)
    private User sourceUser;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "due_user_id", insertable = false, updatable = false)
    private User dueUser;

    @Column(name = "split_amount")
    private Double splitAmount;

    @Column(name = "settled_amount")
    private Double settledAmount;

    @Column(name = "transaction_amount")
    @Min(value = 0)
    private Double transactionAmount;

    @Column(name = "transaction_name")
    private String transactionName;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Date created_at;

    public String getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(String sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public String getDueUserId() {
        return dueUserId;
    }

    public void setDueUserId(String dueUserId) {
        this.dueUserId = dueUserId;
    }

    public User getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(User sourceUser) {
        this.sourceUser = sourceUser;
    }

    public User getDueUser() {
        return dueUser;
    }

    public void setDueUser(User dueUser) {
        this.dueUser = dueUser;
    }

    public Double getSplitAmount() {
        return splitAmount;
    }

    public void setSplitAmount(Double splitAmount) {
        this.splitAmount = splitAmount;
    }

    public Double getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(Double settledAmount) {
        this.settledAmount = settledAmount;
    }

    public Double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(Double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }
}
