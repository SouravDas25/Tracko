package com.trako.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "budget_months")
@Getter
@Setter
public class BudgetMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "budget_month")
    private Integer month;

    @NotNull
    @Column(name = "budget_year")
    private Integer year;

    @NotNull
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "total_budget")
    private Double totalBudget = 0.0;

    @Column(name = "is_closed")
    private Boolean isClosed = false;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;
}
