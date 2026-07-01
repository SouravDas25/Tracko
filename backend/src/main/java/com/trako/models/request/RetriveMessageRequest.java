package com.trako.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetriveMessageRequest {
    private String chatGroup;
    private String retrieveFrom;
}
