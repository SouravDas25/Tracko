package com.trako.dtos;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
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

    public TransactionDetailDTO() {
    }

    public TransactionDetailDTO(Transaction transaction, Category category, Account account, List<SplitDetailDTO> splits) {
        this.id = transaction.getId();
        this.transactionType = transaction.getRenderedTransactionType();
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
}
