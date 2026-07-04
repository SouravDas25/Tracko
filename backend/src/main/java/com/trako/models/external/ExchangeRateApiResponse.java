package com.trako.models.external;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateApiResponse {
    private String baseCode;
    @JsonDeserialize(contentAs = Double.class)
    private Map<String, Double> rates;
}
