package com.trako.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SplitSettleRequest {

    @Min(0)
    @NotNull
    private Double amount;

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
