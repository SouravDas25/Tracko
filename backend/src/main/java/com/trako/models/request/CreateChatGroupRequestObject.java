package com.trako.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateChatGroupRequestObject {
    String name;
    List<String> users;
}
