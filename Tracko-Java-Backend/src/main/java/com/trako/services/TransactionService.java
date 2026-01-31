package com.trako.services;

import com.trako.entities.Transaction;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> findByUserId(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> findByUserIdAndDateBetween(String userId, Date startDate, Date endDate) {
        return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> findByCategoryId(Long categoryId) {
        return transactionRepository.findByCategoryId(categoryId);
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void delete(Long id) {
        transactionRepository.deleteById(id);
    }
}
