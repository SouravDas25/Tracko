package com.trako.entities;

import com.trako.enums.Frequency;
import com.trako.enums.TransactionType;
import com.trako.enums.TransactionTypeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @NotBlank
    @Size(max = 128)
    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "amount", insertable = false, updatable = false)
    private Double amount;

    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "originalCurrency must be a 3-letter uppercase currency code")
    @Column(name = "original_currency", length = 3, nullable = false)
    private String originalCurrency;

    @NotNull
    @Positive
    @Column(name = "original_amount", nullable = false)
    private Double originalAmount;

    @Column(name = "exchange_rate")
    @Positive
    private Double exchangeRate;

    @NotNull
    @Positive
    @Column(name = "account_id")
    private Long accountId;

    @NotNull
    @Positive
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "to_account_id")
    @Positive
    private Long toAccountId;

    @NotNull
    @Column(name = "transaction_type")
    @Convert(converter = TransactionTypeConverter.class)
    private TransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 20)
    private Frequency frequency;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    private Date startDate;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "next_run_date")
    private Date nextRunDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_run_date")
    private Date lastRunDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;
}
