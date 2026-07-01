package com.trako.models.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SplitIndexResponse {
    private List<SplitUserResponse> splitUserResponseList;
}
