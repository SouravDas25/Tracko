package com.trako.models.responses;

import java.util.List;

public class SplitIndexResponse {

    private List<SplitUserResponse> splitUserResponseList;

    public List<SplitUserResponse> getSplitUserResponseList() {
        return splitUserResponseList;
    }

    public void setSplitUserResponseList(List<SplitUserResponse> splitUserResponseList) {
        this.splitUserResponseList = splitUserResponseList;
    }
}
