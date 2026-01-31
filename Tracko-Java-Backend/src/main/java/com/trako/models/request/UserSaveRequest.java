package com.trako.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public class UserSaveRequest {

    private String name;

    @NotNull
    private String phoneNo;

    private String email;

    private String profilePic;

    @NotNull
    @JsonProperty("uuid")
    private String firebase_uuid;

    private Integer isShadow;

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

    public Integer getIsShadow() {
        return isShadow;
    }

    public void setIsShadow(Integer isShadow) {
        this.isShadow = isShadow;
    }

    public boolean isShadow() {
        return isShadow == 1;
    }

    @Override
    public String toString() {
        return "User{" +
                ", name='" + name + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", firebase_uuid='" + firebase_uuid + '\'' +
                '}';
    }
}