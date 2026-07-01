package com.trako.dtos;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TransactionsPageDTO {
    private Integer month;
    private Integer year;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
    @ArraySchema(schema = @Schema(implementation = TransactionDetailDTO.class))
    private List<Object> transactions;

    public void setTransactions(List<?> transactions) {
        this.transactions = (List<Object>) transactions;
    }
}
