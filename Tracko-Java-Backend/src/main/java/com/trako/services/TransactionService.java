package com.trako.services;

import com.trako.entities.Transaction;
import com.trako.repositories.TransactionRepository;
import com.trako.dtos.TransactionSummaryDTO;
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

    public List<Transaction> findByUserIdAndDateBetweenAndAccountIds(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        return transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
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

    public TransactionSummaryDTO getSummary(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        
        double totalIncome = 0.0;
        double totalExpense = 0.0;
        int count = 0;
        
        for (Transaction t : transactions) {
            if (t.getIsCountable() == 1) {
                count++;
                if (t.getTransactionType() == 2) { // CREDIT = income
                    totalIncome += t.getAmount();
                } else if (t.getTransactionType() == 1) { // DEBIT = expense
                    totalExpense += t.getAmount();
                }
            }
        }
        
        double netTotal = totalIncome - totalExpense;
        return new TransactionSummaryDTO(totalIncome, totalExpense, netTotal, count);
    }

    public TransactionSummaryDTO getSummary(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        List<Transaction> transactions;
        if (accountIds == null || accountIds.isEmpty()) {
            transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        } else {
            transactions = transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
        }

        double totalIncome = 0.0;
        double totalExpense = 0.0;
        int count = 0;

        for (Transaction t : transactions) {
            if (t.getIsCountable() == 1) {
                count++;
                if (t.getTransactionType() == 2) { // CREDIT = income
                    totalIncome += t.getAmount();
                } else if (t.getTransactionType() == 1) { // DEBIT = expense
                    totalExpense += t.getAmount();
                }
            }
        }

        double netTotal = totalIncome - totalExpense;
        return new TransactionSummaryDTO(totalIncome, totalExpense, netTotal, count);
    }

    public Double getTotalIncome(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == 2) // CREDIT = income
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Double getTotalExpense(String userId, Date startDate, Date endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == 1) // DEBIT = expense
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
}
