package com.trako.models.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SplitUserResponse {
    private List<SplitResponse> splits;
    private String id;
    private String name;
    private String phoneNo;
    private String email;
}
