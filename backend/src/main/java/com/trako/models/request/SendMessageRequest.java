package com.trako.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
    private String sender;
    private String chatGroupAddress;
    private String message;
}
