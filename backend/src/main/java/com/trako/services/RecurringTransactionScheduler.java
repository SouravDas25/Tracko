package com.trako.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void processRecurringTransactions() {
        recurringTransactionService.processDueTransactions();
    }
}
