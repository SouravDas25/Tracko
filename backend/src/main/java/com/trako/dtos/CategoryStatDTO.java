package com.trako.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatDTO {
    private Long categoryId;
    private String categoryName;
    private Double amount;
}
