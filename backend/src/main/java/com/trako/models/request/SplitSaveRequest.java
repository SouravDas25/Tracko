package com.trako.models.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

public class SplitSaveRequest {

    @NotNull
    @JsonProperty("userId")
    private String dueUserId;

    @NotNull
    @Min(value = 0)
    @JsonProperty("amount")
    private Double splitAmount;

    @Min(value = 0)
    private Double transactionAmount;

    private String transactionName;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date created_at;

    public String getDueUserId() {
        return dueUserId;
    }

    public void setDueUserId(String dueUserId) {
        this.dueUserId = dueUserId;
    }

    public Double getSplitAmount() {
        return splitAmount;
    }

    public void setSplitAmount(Double splitAmount) {
        this.splitAmount = splitAmount;
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
