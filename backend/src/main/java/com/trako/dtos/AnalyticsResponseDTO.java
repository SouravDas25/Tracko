package com.trako.dtos;

import com.trako.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDTO {
    private String granularity;
    private TransactionType transactionType;
    private String periodStart;
    private String periodEnd;
    private Double total;
    private List<NamedSeriesDTO> groupedSeries;
}
