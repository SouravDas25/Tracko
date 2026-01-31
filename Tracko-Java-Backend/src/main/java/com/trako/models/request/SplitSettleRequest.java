package com.trako.models.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
