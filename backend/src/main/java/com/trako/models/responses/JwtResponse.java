package com.trako.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class JwtResponse implements Serializable {

    @JsonProperty("token")
    private final String jwttoken;

    public JwtResponse(String jwttoken) {
        this.jwttoken = jwttoken;
    }

    public String getToken() {
        return this.jwttoken;
    }
}
