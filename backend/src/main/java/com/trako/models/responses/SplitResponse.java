package com.trako.models.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SplitResponse {
    private String id;
    private Double splitAmount;
    private Double settledAmount;
    private Double transactionAmount;
    private String transactionName;
    private Date created_at;
}
