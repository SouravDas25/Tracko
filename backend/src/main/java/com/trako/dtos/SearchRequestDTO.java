package com.trako.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SearchRequestDTO {
    @NotBlank(message = "Search query cannot be blank")
    @Size(min = 1, max = 200, message = "Search query must be between 1 and 200 characters")
    private String query;

    @PositiveOrZero(message = "Page must be zero or positive")
    private Integer page = 0;

    @Positive(message = "Page size must be positive")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 20;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    @DecimalMin(value = "0.0", message = "Minimum amount cannot be negative")
    private Double minAmount;

    @DecimalMin(value = "0.0", message = "Maximum amount cannot be negative")
    private Double maxAmount;

    private List<Long> accountIds;

    private Long categoryId;

    @DecimalMin(value = "0.0", message = "Fuzzy threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Fuzzy threshold must be between 0.0 and 1.0")
    private Double fuzzyThreshold = 0.7;

    private Boolean expand = false;
}
