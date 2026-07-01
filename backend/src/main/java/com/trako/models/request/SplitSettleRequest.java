package com.trako.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitSettleRequest {
    @Min(0)
    @NotNull
    private Double amount;
}
