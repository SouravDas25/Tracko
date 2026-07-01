package com.trako.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class JwtResponse implements Serializable {

    @JsonProperty("token")
    private final String jwttoken;

    public JwtResponse(String jwttoken) {
        this.jwttoken = jwttoken;
    }
}
