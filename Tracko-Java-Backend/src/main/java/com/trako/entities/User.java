package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

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

    @Column(name = "firebase_uuid")
    private String firebase_uuid;

    @Column(name = "is_shadow")
    private Integer isShadow;

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserChatGroup> groups;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<NlpData> nlpData;

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

    public String getFirebase_uuid() {
        return firebase_uuid;
    }

    public void setFirebase_uuid(String firebase_uuid) {
        this.firebase_uuid = firebase_uuid;
    }

    public Set<UserChatGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<UserChatGroup> groups) {
        this.groups = groups;
    }

    public Integer getIsShadow() {
        return isShadow;
    }

    public void setIsShadow(Integer isShadow) {
        this.isShadow = isShadow;
    }

    public boolean isShadow() {
        return isShadow == 1;
    }

    public List<NlpData> getNlpData() {
        return nlpData;
    }

    public void setNlpData(List<NlpData> nlpData) {
        this.nlpData = nlpData;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", firebase_uuid='" + firebase_uuid + '\'' +
                '}';
    }


}