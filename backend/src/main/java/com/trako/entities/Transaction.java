package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionEntryTypeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.util.Date;

@Entity
@Table(name = "transactions")
@Getter
@Setter
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

    @JsonIgnore
    public TransactionDbType getTransactionType() {
        return transactionType;
    }

    @JsonProperty("transactionType")
    public Integer getRenderedTransactionType() {
        return renderedTransactionType != null ? renderedTransactionType : (transactionType != null ? transactionType.getValue() : null);
    }
}
