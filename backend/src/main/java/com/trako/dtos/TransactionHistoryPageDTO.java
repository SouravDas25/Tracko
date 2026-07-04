package com.trako.dtos;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A page of transaction change-history entries (most recent first) with the paging metadata a
 * client needs to load further pages of a potentially large history.
 */
@Getter
@Setter
public class TransactionHistoryPageDTO {
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
    @ArraySchema(schema = @Schema(implementation = TransactionHistoryDTO.class))
    private List<TransactionHistoryDTO> history;

    public static TransactionHistoryPageDTO from(Page<TransactionHistoryDTO> page) {
        TransactionHistoryPageDTO dto = new TransactionHistoryPageDTO();
        dto.setPage(page.getNumber());
        dto.setSize(page.getSize());
        dto.setTotalElements(page.getTotalElements());
        dto.setTotalPages(page.getTotalPages());
        dto.setHasNext(page.hasNext());
        dto.setHasPrevious(page.hasPrevious());
        dto.setHistory(page.getContent());
        return dto;
    }
}
