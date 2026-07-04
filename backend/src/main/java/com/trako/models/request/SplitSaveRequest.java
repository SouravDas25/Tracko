package com.trako.models.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
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
}
