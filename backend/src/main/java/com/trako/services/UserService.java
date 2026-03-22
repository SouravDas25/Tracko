package com.trako.services;

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
    CurrencyService currencyService;

    @Autowired
    BudgetMonthRepository budgetMonthRepository;

    @Autowired
    BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    AllocationRuleRepository allocationRuleRepository;

    @Autowired
    RecurringTransactionRepository recurringTransactionRepository;

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

        // Reject if email is already taken by a different user
        if (userSaveRequest.getEmail() != null && !userSaveRequest.getEmail().isBlank()) {
            User emailOwner = usersRepository.findByEmail(userSaveRequest.getEmail());
            if (emailOwner != null && (isPreset == null || !emailOwner.getId().equals(isPreset.getId()))) {
                log.warn("User save rejected: email {} already in use", userSaveRequest.getEmail());
                return null;
            }
        }

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

        // 1. Delete Splits associated with user's transactions
        splitRepository.deleteByTransactionUserId(userId);

        // 2. Delete Splits by userId (cleanup)
        splitRepository.deleteByUserId(userId);

        // 3. Delete Transactions
        transactionRepository.deleteByUserId(userId);

        // 4. Delete Budget Allocations
        budgetCategoryAllocationRepository.deleteByUserId(userId);

        // 5. Delete Budget Months
        budgetMonthRepository.deleteByUserId(userId);

        // 6. Delete Allocation Rules
        allocationRuleRepository.deleteByUserId(userId);

        // 7. Delete Contacts
        contactRepository.deleteByUserId(userId);

        // 8. Delete User Currencies
        currencyService.deleteAllForUser(userId);

        // 9. Delete Recurring Transactions
        recurringTransactionRepository.deleteByUserId(userId);

        // 10. Delete Accounts
        accountRepository.deleteByUserId(userId);

        // 11. Delete Categories
        categoryRepository.deleteByUserId(userId);

        log.info("Data reset completed for user: {}", userId);
    }

    @Transactional
    public void resetUserTransactions(String userId) {
        log.info("Resetting transactions for user: {}", userId);

        // 1. Delete Splits associated with user's transactions
        splitRepository.deleteByTransactionUserId(userId);

        // 2. Delete Transactions
        transactionRepository.deleteByUserId(userId);

        // 3. Reset Budget Allocations (Actual Spent = 0, Remaining = Allocated)
        budgetCategoryAllocationRepository.resetActualSpentByUserId(userId);

        log.info("Transactions reset completed for user: {}", userId);
    }
}
