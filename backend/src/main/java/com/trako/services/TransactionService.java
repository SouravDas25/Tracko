package com.trako.services;

import com.trako.dtos.SplitDetailDTO;
import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private SplitRepository splitRepository;

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

    public Page<Transaction> findByUserIdAndDateBetween(String userId, Date startDate, Date endDate, Pageable pageable) {
        return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate, pageable);
    }

    public List<Transaction> findByUserIdAndDateBetweenAndAccountIds(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        return transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
    }

    public List<TransactionDetailDTO> findWithDetailsByUserIdAndDateBetween(String userId, Date startDate, Date endDate, List<Long> accountIds) {
        List<Transaction> transactions;
        if (accountIds == null || accountIds.isEmpty()) {
            transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        } else {
            transactions = transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, startDate, endDate, accountIds);
        }

        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect IDs
        Set<Long> acctIds = new HashSet<>();
        Set<Long> catIds = new HashSet<>();
        List<Long> txIds = new ArrayList<>();

        for (Transaction t : transactions) {
            acctIds.add(t.getAccountId());
            catIds.add(t.getCategoryId());
            txIds.add(t.getId());
        }

        // Batch Fetch
        List<Account> accounts = accountRepository.findAllById(acctIds);
        Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));

        List<Category> categories = categoryRepository.findAllById(catIds);
        Map<Long, Category> categoryMap = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Split> splits = splitRepository.findByTransactionIdIn(txIds);
        Map<Long, List<Split>> splitsByTxId = splits.stream().collect(Collectors.groupingBy(Split::getTransactionId));

        // Fetch Contacts for Splits
        Set<Long> contactIds = splits.stream()
                .map(Split::getContactId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Contact> contacts = contactRepository.findAllById(contactIds);
        Map<Long, Contact> contactMap = contacts.stream().collect(Collectors.toMap(Contact::getId, Function.identity()));

        // Assemble DTOs
        List<TransactionDetailDTO> dtos = new ArrayList<>();
        for (Transaction t : transactions) {
            Account acct = accountMap.get(t.getAccountId());
            Category cat = categoryMap.get(t.getCategoryId());
            List<Split> txSplits = splitsByTxId.getOrDefault(t.getId(), Collections.emptyList());

            List<SplitDetailDTO> splitdtos = txSplits.stream().map(s -> {
                Contact c = (s.getContactId() != null) ? contactMap.get(s.getContactId()) : null;
                return new SplitDetailDTO(s, c);
            }).collect(Collectors.toList());

            dtos.add(new TransactionDetailDTO(t, cat, acct, splitdtos));
        }

        return dtos;
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> findByCategoryId(Long categoryId) {
        return transactionRepository.findByCategoryId(categoryId);
    }

    public Transaction save(Transaction transaction) {
        if (transaction.getAmount() == null) {
            if (transaction.getOriginalAmount() != null && transaction.getExchangeRate() != null) {
                // Calculate amount based on original amount and exchange rate
                // Assuming exchangeRate is: 1 Unit of Original = X Units of Base
                double calculatedAmount = transaction.getOriginalAmount() * transaction.getExchangeRate();
                // Round to 2 decimal places
                transaction.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
            } else if (transaction.getOriginalAmount() != null && transaction.getOriginalCurrency() != null) {
                // No exchangeRate provided: fetch from user's configured rates
                String userId;
                try {
                    userId = userService.loggedInUser().getId();
                } catch (UserNotLoggedInException e) {
                    throw new RuntimeException("User not logged in", e);
                }
                String currencyCode = transaction.getOriginalCurrency().toUpperCase();
                UserCurrency uc = userCurrencyRepository.findByUserIdAndCurrencyCode(userId, currencyCode);
                if (uc != null && uc.getExchangeRate() != null) {
                    double calculatedAmount = transaction.getOriginalAmount() * uc.getExchangeRate();
                    transaction.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
                    transaction.setExchangeRate(uc.getExchangeRate());
                } else {
                    throw new IllegalArgumentException("No exchange rate configured for currency: " + currencyCode);
                }
            } else {
                throw new IllegalArgumentException("Amount cannot be null unless originalAmount and either exchangeRate or originalCurrency are provided");
            }
        }
        
        // Ensure consistency if both are provided? 
        // For now, trust the calculated or provided amount, but maybe we should re-calculate if original is present?
        // Let's stick to: if original info is present, ensure amount matches?
        // Or just lenient: if amount is present, use it. If not, calculate.
        
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

    public TransactionSummaryDTO getSummaryWithRollover(String userId, Date startDate, Date endDate, List<Long> accountIds) {
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
        double rolloverNet = calculateRolloverNet(userId, startDate, accountIds);
        double netTotalWithRollover = netTotal + rolloverNet;
        return new TransactionSummaryDTO(totalIncome, totalExpense, netTotal, rolloverNet, netTotalWithRollover, count);
    }

    private double calculateRolloverNet(String userId, Date periodStartDate, List<Long> accountIds) {
        return calculateRolloverNetInternal(userId, periodStartDate, accountIds, 36);
    }

    private double calculateRolloverNetInternal(String userId, Date periodStartDate, List<Long> accountIds, int maxDepth) {
        if (maxDepth <= 0) return 0.0;
        if (periodStartDate == null) return 0.0;

        Calendar cal = Calendar.getInstance();
        cal.setTime(periodStartDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date prevStart = cal.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(prevStart);
        calEnd.add(Calendar.MONTH, 1);
        Date prevEnd = calEnd.getTime();

        List<Transaction> prevTransactions;
        if (accountIds == null || accountIds.isEmpty()) {
            prevTransactions = transactionRepository.findByUserIdAndDateBetween(userId, prevStart, prevEnd);
        } else {
            prevTransactions = transactionRepository.findByUserIdAndDateBetweenAndAccountIds(userId, prevStart, prevEnd, accountIds);
        }

        if (prevTransactions == null || prevTransactions.isEmpty()) {
            return 0.0;
        }

        double prevIncome = 0.0;
        double prevExpense = 0.0;

        for (Transaction t : prevTransactions) {
            if (t.getIsCountable() == 1) {
                if (t.getTransactionType() == 2) {
                    prevIncome += t.getAmount();
                } else if (t.getTransactionType() == 1) {
                    prevExpense += t.getAmount();
                }
            }
        }

        double prevNet = prevIncome - prevExpense;
        double earlier = calculateRolloverNetInternal(userId, prevStart, accountIds, maxDepth - 1);
        return prevNet + earlier;
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
