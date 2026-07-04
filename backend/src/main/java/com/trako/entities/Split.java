package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "splits")
@Getter
@Setter
public class Split {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "transaction_id")
    private Long transactionId;

    @NotNull
    @Column(name = "user_id")
    private String userId;

    @NotNull
    @jakarta.validation.constraints.Positive
    @Column(name = "amount")
    private Double amount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "settled_at")
    private Date settledAt;

    @Column(name = "is_settled")
    private Integer isSettled = 0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    private Transaction transaction;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "contact_id")
    private Long contactId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", insertable = false, updatable = false)
    private Contact contact;
}
