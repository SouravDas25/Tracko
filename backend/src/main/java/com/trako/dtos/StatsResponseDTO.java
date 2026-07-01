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
public class StatsResponseDTO {
    private String range;
    private TransactionType transactionType;
    private String periodStart;
    private String periodEnd;
    private Double total;
    private List<StatsPointDTO> series;
    private List<CategoryStatDTO> categories;
}
