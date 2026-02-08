package com.trako.models.responses;

import java.util.List;

public class SplitUserResponse {

    private List<SplitResponse> splits;
    private String id;
    private String name;
    private String phoneNo;
    private String email;

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

    public List<SplitResponse> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitResponse> splits) {
        this.splits = splits;
    }
}
