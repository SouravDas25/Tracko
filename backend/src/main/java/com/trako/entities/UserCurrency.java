package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_currencies")
@Getter
@Setter
public class UserCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotNull
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Column(name = "exchange_rate", nullable = false)
    private Double exchangeRate;

    @Override
    public String toString() {
        return "UserCurrency{" +
                "id=" + id +
                ", currencyCode='" + currencyCode + '\'' +
                ", exchangeRate=" + exchangeRate +
                '}';
    }
}
