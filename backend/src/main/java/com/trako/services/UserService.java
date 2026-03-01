package com.trako.services;

import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.UserSaveRequest;
import com.trako.repositories.*;
import com.trako.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    SplitRepository splitRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ContactRepository contactRepository;

    @Autowired
    UserCurrencyRepository userCurrencyRepository;

    @Autowired
    BudgetMonthRepository budgetMonthRepository;

    @Autowired
    BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    AllocationRuleRepository allocationRuleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public User loggedInUser() throws UserNotLoggedInException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String phoneNo = null;
        if (principal instanceof UserDetails) {
            phoneNo = ((UserDetails) principal).getUsername();
            return findByPhoneNo(phoneNo);
        } else {
            throw new UserNotLoggedInException();
        }

    }

    public List<User> findUser(String id) {
        if (id == null) {
            return usersRepository.findAll();
        }
        return Collections.singletonList(usersRepository.findById(id).orElse(null));
    }

    public User findByPhoneNo(String phoneNo) {
        phoneNo = CommonUtil.extractPhoneNumber(phoneNo);
        return usersRepository.findByPhoneNo(phoneNo);
    }

    public User findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return usersRepository.findById(id).orElse(null);
    }

    public String save(UserSaveRequest userSaveRequest) {

        log.info("User Save Request : {}", userSaveRequest);
        String phnNo = CommonUtil.extractPhoneNumber(userSaveRequest.getPhoneNo());
        if (phnNo == null) {
            return null;
        }

        User user;
        User isPreset = usersRepository.findByPhoneNo(phnNo);
        if (isPreset != null) {
            user = CommonUtil.mapModel(isPreset, User.class);
        } else {
            user = new User();
        }
        if (!userSaveRequest.isShadow() || isPreset == null) {
            CommonUtil.mapModel(userSaveRequest, user);
        }

        user.setPhoneNo(phnNo);


        if (isPreset != null) {
            user.setId(isPreset.getId());
        } else {
            user.setId(null);
        }
        if (user.isShadow() && isPreset != null) {
            if (isPreset.getEmail() != null && user.getEmail() == null)
                user.setEmail(isPreset.getEmail());
            if (isPreset.getName() != null && user.getName() == null)
                user.setName(isPreset.getName());
        }

        String rawPassword = userSaveRequest.getPassword();
        boolean hasPassword = rawPassword != null && !rawPassword.trim().isEmpty();

        // For a brand new user, password must be provided.
        // For updates, allow missing password to keep existing password unchanged.
        if (isPreset == null && !hasPassword) {
            log.warn("User create rejected: missing password for phoneNo={}", phnNo);
            return null;
        }

        if (hasPassword) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        } else if (isPreset != null && isPreset.getPassword() != null) {
            user.setPassword(isPreset.getPassword());
        }

        if (userSaveRequest.getBaseCurrency() != null) {
            user.setBaseCurrency(userSaveRequest.getBaseCurrency());
        } else if (user.getBaseCurrency() == null) {
            user.setBaseCurrency("INR");
        }
        
        user = usersRepository.save(user);
        return user.getId();
    }

    public User saveUser(User user) {
        return usersRepository.save(user);
    }

    @Transactional
    public void resetUserData(String userId) {
        log.info("Resetting data for user: {}", userId);

        // 1. Get all accounts for the user
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = userAccounts.stream().map(Account::getId).collect(Collectors.toList());

        // 2. Get all transactions for these accounts
        if (!accountIds.isEmpty()) {
            List<Transaction> userTransactions = transactionRepository.findByAccountIdIn(accountIds);
            List<Long> transactionIds = userTransactions.stream().map(Transaction::getId).collect(Collectors.toList());

            // 3. Delete Splits associated with these transactions
            if (!transactionIds.isEmpty()) {
                splitRepository.deleteByTransactionIdIn(transactionIds);
            }
            
            // 4. Delete Transactions
            transactionRepository.deleteByAccountIdIn(accountIds);
        }

        // 5. Delete Splits by userId (cleanup)
        splitRepository.deleteByUserId(userId);

        // 6. Delete Budget Allocations
        budgetCategoryAllocationRepository.deleteByUserId(userId);

        // 7. Delete Budget Months
        budgetMonthRepository.deleteByUserId(userId);

        // 8. Delete Allocation Rules
        allocationRuleRepository.deleteByUserId(userId);

        // 9. Delete Contacts
        contactRepository.deleteByUserId(userId);

        // 10. Delete User Currencies
        userCurrencyRepository.deleteByUserId(userId);

        // 11. Delete Accounts
        accountRepository.deleteByUserId(userId);

        // 12. Delete Categories
        categoryRepository.deleteByUserId(userId);

        log.info("Data reset completed for user: {}", userId);
    }

    @Transactional
    public void resetUserTransactions(String userId) {
        log.info("Resetting transactions for user: {}", userId);

        // 1. Get all accounts for the user
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = userAccounts.stream().map(Account::getId).collect(Collectors.toList());

        // 2. Get all transactions for these accounts
        if (!accountIds.isEmpty()) {
            List<Transaction> userTransactions = transactionRepository.findByAccountIdIn(accountIds);
            List<Long> transactionIds = userTransactions.stream().map(Transaction::getId).collect(Collectors.toList());

            // 3. Delete Splits associated with these transactions
            if (!transactionIds.isEmpty()) {
                splitRepository.deleteByTransactionIdIn(transactionIds);
            }

            // 4. Delete Transactions
            transactionRepository.deleteByAccountIdIn(accountIds);
        }

        // 5. Reset Budget Allocations (Actual Spent = 0, Remaining = Allocated)
        budgetCategoryAllocationRepository.resetActualSpentByUserId(userId);

        log.info("Transactions reset completed for user: {}", userId);
    }
}
