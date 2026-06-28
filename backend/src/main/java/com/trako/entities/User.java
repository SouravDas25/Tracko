package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends AbstractBaseEntity {

    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "phone_no", unique = true)
    private String phoneNo;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "profile_pic")
    private String profilePic;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @Column(name = "global_id", length = 64, unique = true)
    private String globalId;

    @Column(name = "base_currency", length = 3)
    private String baseCurrency = "INR";

    @Column(name = "is_shadow")
    private Integer isShadow;

    @Column(name = "is_admin")
    private Integer isAdmin = 0;

    @JsonIgnore
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @JsonIgnore
    @Column(name = "lockout_count")
    private Integer lockoutCount = 0;

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lock_until")
    private Date lockUntil;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCurrency> secondaryCurrencies;

    public boolean isShadow() {
        return isShadow != null && isShadow == 1;
    }

    public boolean isAdmin() {
        return isAdmin != null && isAdmin == 1;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
