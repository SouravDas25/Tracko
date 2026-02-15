package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends AbstractBaseEntity {

    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "phone_no")
    private String phoneNo;

    @Column(name = "email")
    private String email;

    @Column(name = "profile_pic")
    private String profilePic;

    @Column(name = "fire_base_id")
    private String fireBaseId;

    @Column(name = "global_id", length = 64, unique = true)
    private String globalId;

    @Column(name = "base_currency", length = 3)
    private String baseCurrency = "INR";

    @Column(name = "is_shadow")
    private Integer isShadow;

    @Column(name = "is_admin")
    private Integer isAdmin = 0;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getFireBaseId() {
        return fireBaseId;
    }

    public void setFireBaseId(String fireBaseId) {
        this.fireBaseId = fireBaseId;
    }

    public String getGlobalId() {
        return globalId;
    }

    public void setGlobalId(String globalId) {
        this.globalId = globalId;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public Integer getIsShadow() {
        return isShadow;
    }

    public void setIsShadow(Integer isShadow) {
        this.isShadow = isShadow;
    }

    public boolean isShadow() {
        return isShadow != null && isShadow == 1;
    }

    public Integer getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Integer isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean isAdmin() {
        return isAdmin != null && isAdmin == 1;
    }

    public List<UserCurrency> getSecondaryCurrencies() {
        return secondaryCurrencies;
    }

    public void setSecondaryCurrencies(List<UserCurrency> secondaryCurrencies) {
        this.secondaryCurrencies = secondaryCurrencies;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", fireBaseId='" + fireBaseId + '\'' +
                '}';
    }


}