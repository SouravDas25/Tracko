package com.trako.services;

import com.trako.dtos.SearchRequestDTO;
import com.trako.dtos.TransactionSearchResultDTO;

import java.util.List;

public interface TransactionSearchService {

    TransactionSearchResultDTO search(String userId, SearchRequestDTO request);

    List<String> tokenizeQuery(String query);

    String normalizeQuery(String query);
}
