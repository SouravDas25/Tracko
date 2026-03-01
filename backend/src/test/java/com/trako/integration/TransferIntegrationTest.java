package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Integration tests for transfer operations.
 * 
 * Tests verify complete API flow by calling actual HTTP endpoints:
 * - POST /api/transactions (with toAccountId) - Create transfer
 * - DELETE /api/transactions/{id} - Delete transfer
 * - Verifies HTTP status codes
 * - Validates response bodies
 * - Checks database state after API calls
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private Account sourceAccount;
    private Account destinationAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        // Create test user
        testUser = new User();
        testUser.setId("test-user-" + System.currentTimeMillis());
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNo("1234567890");
        testUser.setPassword("firebase-" + System.currentTimeMillis());
        testUser = userRepository.save(testUser);

        // Generate JWT token for authentication
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        // Create source account
        sourceAccount = new Account();
        sourceAccount.setName("Source Account");
        sourceAccount.setUserId(testUser.getId());
        sourceAccount.setCurrency("USD");
        sourceAccount = accountRepository.save(sourceAccount);

        // Create destination account
        destinationAccount = new Account();
        destinationAccount.setName("Destination Account");
        destinationAccount.setUserId(testUser.getId());
        destinationAccount.setCurrency("USD");
        destinationAccount = accountRepository.save(destinationAccount);
        
        // Create test category for regular transaction tests
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);
    }

    /**
     * Test: Create a transfer successfully via API
     * 
     * Scenario: User creates a transfer between two accounts
     * Expected: Transfer creates two linked transactions (debit + credit) atomically
     */
    @Test
    public void testCreateTransferViaAPI_Success() throws Exception {
        // GIVEN: Prepare transfer request payload
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        transferRequest.put("toAccountId", destinationAccount.getId());  // Destination: money comes in (makes it a transfer)
        // New contract: provide currency fields; base currency assumed to resolve rate=1.0
        transferRequest.put("originalAmount", 500.0);
        transferRequest.put("originalCurrency", "INR");
        transferRequest.put("name", "API Test Transfer");  // Optional name
        transferRequest.put("comments", "Integration test via API");  // Optional comments

        // WHEN: Call POST /api/transactions endpoint (detects transfer by presence of toAccountId)
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                // THEN: Verify HTTP response
                .andExpect(status().isOk())  // Should return 200 OK
                .andExpect(jsonPath("$.message", containsString("successfully")))  // Success message
                .andReturn();

        // VERIFY: Check database state after API call
        List<Transaction> allTransactions = transactionRepository.findAll();
        // Filter to get only transfer transactions (those with linkedTransactionId)
        List<Transaction> transferTransactions = allTransactions.stream()
                .filter(t -> t.getLinkedTransactionId() != null)
                .toList();

        // Should have exactly 2 transactions (debit + credit)
        assertEquals(2, transferTransactions.size(), "Should have 2 linked transactions");

        // VERIFY: Find and validate debit transaction (TYPE_DEBIT = 1)
        Transaction debit = transferTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)  // Find debit (money out)
                .findFirst()
                .orElse(null);
        // VERIFY: Find and validate credit transaction (TYPE_CREDIT = 2)
        Transaction credit = transferTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CREDIT)  // Find credit (money in)
                .findFirst()
                .orElse(null);

        assertNotNull(debit, "Debit transaction should exist");
        assertNotNull(credit, "Credit transaction should exist");

        // VERIFY: Bidirectional linking (debit ↔ credit)
        assertEquals(credit.getId(), debit.getLinkedTransactionId(), 
                    "Debit should link to credit");
        assertEquals(debit.getId(), credit.getLinkedTransactionId(), 
                    "Credit should link back to debit");

        // VERIFY: Both transactions have same amount
        assertEquals(500.0, debit.getAmount(), "Debit amount should be 500");
        assertEquals(500.0, credit.getAmount(), "Credit amount should be 500");

        // VERIFY: Transactions are on correct accounts
        assertEquals(sourceAccount.getId(), debit.getAccountId(), 
                    "Debit should be on source account");
        assertEquals(destinationAccount.getId(), credit.getAccountId(), 
                    "Credit should be on destination account");

        // VERIFY: Transfers are non-countable (don't affect account summaries)
        assertEquals(0, debit.getIsCountable(), "Debit should be non-countable");
        assertEquals(0, credit.getIsCountable(), "Credit should be non-countable");
    }

    /**
     * Test: Attempt to create transfer with same source and destination account
     * 
     * Scenario: User tries to transfer money from Account A to Account A
     * Expected: API rejects with 400 Bad Request and appropriate error message
     */
    @Test
    public void testCreateTransferViaAPI_SameAccount_ReturnsBadRequest() throws Exception {
        // GIVEN: Prepare invalid transfer request (same account)
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        transferRequest.put("toAccountId", sourceAccount.getId());  // Same as source - INVALID!
        transferRequest.put("originalAmount", 500.0);
        transferRequest.put("originalCurrency", "INR");
        transferRequest.put("name", "Invalid Transfer");

        // WHEN: Call POST /api/transactions with invalid data
        // THEN: Should reject with 400 Bad Request
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest())  // Should return 400
                .andExpect(jsonPath("$.message", containsString("same")));  // Error message

        // VERIFY: No transactions should be created in database
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertEquals(0, allTransactions.size(), "No transactions should be created");
    }

    /**
     * Test: Attempt to create transfer with non-existent account
     * 
     * Scenario: User tries to transfer to an account that doesn't exist
     * Expected: API rejects with 400 Bad Request
     */
    @Test
    public void testCreateTransferViaAPI_InvalidAccount_ReturnsBadRequest() throws Exception {
        // GIVEN: Prepare transfer request with non-existent account ID
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field - Valid source
        transferRequest.put("toAccountId", 99999L);  // Non-existent account - INVALID!
        transferRequest.put("originalAmount", 500.0);
        transferRequest.put("originalCurrency", "INR");
        transferRequest.put("name", "Invalid Transfer");

        // WHEN: Call POST /api/transactions with invalid account
        // THEN: Should reject with 400 Bad Request
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest())  // Should return 400
                .andExpect(jsonPath("$.message", any(String.class)));  // Error message

        // VERIFY: No transactions should be created in database
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertEquals(0, allTransactions.size(), "No transactions should be created");
    }

    /**
     * Test: Delete a transfer by deleting the debit side
     * 
     * Scenario: User deletes a transfer by calling DELETE on the debit transaction
     * Expected: BOTH debit AND credit transactions are deleted atomically
     */
    @Test
    public void testDeleteTransferViaAPI_Success() throws Exception {
        // GIVEN: Create a transfer via API first
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        transferRequest.put("toAccountId", destinationAccount.getId());
        transferRequest.put("originalAmount", 750.0);
        transferRequest.put("originalCurrency", "INR");
        transferRequest.put("name", "Transfer to Delete");

        // Create the transfer
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        // Get the created transactions from database
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(2, transactions.size(), "Should have 2 transactions");
        
        // Find the debit transaction (money out)
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)  // TYPE_DEBIT
                .findFirst()
                .orElseThrow();

        Long debitId = debit.getId();
        Long creditId = debit.getLinkedTransactionId();  // Get linked credit ID

        // WHEN: Delete via API using debit transaction ID
        mockMvc.perform(delete("/api/transactions/" + debitId)
                        .header("Authorization", bearerToken))
                // THEN: Should return success
                .andExpect(status().isOk())  // 200 OK
                .andExpect(jsonPath("$.message", containsString("Transfer deleted successfully")));

        // VERIFY: BOTH transactions should be deleted from database
        assertFalse(transactionRepository.findById(debitId).isPresent(), 
                   "Debit transaction should be deleted");
        assertFalse(transactionRepository.findById(creditId).isPresent(), 
                   "Credit transaction should also be deleted (atomic operation)");
    }

    /**
     * Test: Delete a transfer by deleting the credit side
     * 
     * Scenario: User deletes a transfer by calling DELETE on the credit transaction
     * Expected: BOTH debit AND credit transactions are deleted (symmetric behavior)
     */
    @Test
    public void testDeleteTransferViaAPI_ViaCreditSide_Success() throws Exception {
        // GIVEN: Create a transfer via API
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        transferRequest.put("toAccountId", destinationAccount.getId());
        transferRequest.put("originalAmount", 1000.0);
        transferRequest.put("originalCurrency", "USD");
        transferRequest.put("exchangeRate", 1.0);
        transferRequest.put("name", "Transfer to Delete via Credit");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        // Get the created transactions
        List<Transaction> transactions = transactionRepository.findAll();
        // Find the credit transaction (money in)
        Transaction credit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CREDIT)  // TYPE_CREDIT
                .findFirst()
                .orElseThrow();

        Long creditId = credit.getId();
        Long debitId = credit.getLinkedTransactionId();  // Get linked debit ID

        // WHEN: Delete via credit side (instead of debit)
        mockMvc.perform(delete("/api/transactions/" + creditId)
                        .header("Authorization", bearerToken))
                // THEN: Should still delete both transactions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transfer deleted successfully")));

        // VERIFY: Both transactions deleted (symmetric behavior)
        assertFalse(transactionRepository.findById(debitId).isPresent(), 
                   "Debit should be deleted");
        assertFalse(transactionRepository.findById(creditId).isPresent(), 
                   "Credit should be deleted");
    }

    /**
     * Test: Delete a regular (non-transfer) transaction
     * 
     * Scenario: User deletes a normal transaction (not part of a transfer)
     * Expected: Only the single transaction is deleted, no special transfer handling
     */
    @Test
    public void testDeleteRegularTransactionViaAPI_NotAffectingTransfers() throws Exception {
        // GIVEN: Create a regular transaction (NOT a transfer)
        Transaction regular = new Transaction();
        regular.setAccountId(sourceAccount.getId());
        regular.setCategoryId(testCategory.getId());  // Use created test category
        regular.setTransactionType(TransactionType.DEBIT);  // TYPE_DEBIT
        regular.setOriginalAmount(100.0);
        regular.setOriginalCurrency("INR");
        regular.setExchangeRate(1.0);
        regular.setDate(new java.util.Date());
        regular.setIsCountable(1);  // Countable (unlike transfers)
        regular.setName("Regular Transaction");
        // Note: No linkedTransactionId - this is NOT a transfer
        regular = transactionRepository.save(regular);

        Long regularId = regular.getId();

        // WHEN: Delete regular transaction via API
        mockMvc.perform(delete("/api/transactions/" + regularId)
                        .header("Authorization", bearerToken))
                // THEN: Should return success message for "Transaction" (not "Transfer")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transaction deleted successfully")));

        // VERIFY: Only this transaction deleted (no linked transaction)
        assertFalse(transactionRepository.findById(regularId).isPresent());
    }

    /**
     * Test: Create multiple transfers and verify they don't interfere
     * 
     * Scenario: User creates 3 separate transfers in sequence
     * Expected: Each transfer is independent, properly linked, no cross-linking
     */
    @Test
    public void testMultipleTransfersViaAPI_AllIndependent() throws Exception {
        // GIVEN & WHEN: Create 3 transfers with different amounts
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> request = new HashMap<>();
            request.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
            request.put("toAccountId", destinationAccount.getId());
            request.put("originalAmount", 100.0 * i);  // $100, $200, $300
            request.put("originalCurrency", "INR");
            request.put("name", "Transfer " + i);

            // Create each transfer via API
            mockMvc.perform(post("/api/transactions")
                            .header("Authorization", bearerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // VERIFY: All transfers created (3 transfers × 2 transactions each = 6)
        List<Transaction> all = transactionRepository.findAll();
        assertEquals(6, all.size(), "Should have 6 transactions (3 transfers x 2)");

        // VERIFY: Each transfer is properly linked
        List<Transaction> debits = all.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)  // Get all debits
                .toList();
        assertEquals(3, debits.size(), "Should have 3 debit transactions");

        // For each debit, verify it has a corresponding linked credit
        for (Transaction debit : debits) {
            assertNotNull(debit.getLinkedTransactionId(), 
                         "Each debit should have linked transaction");
            
            // Find the linked credit transaction
            Transaction credit = transactionRepository.findById(debit.getLinkedTransactionId())
                    .orElse(null);
            assertNotNull(credit, "Linked credit should exist");
            
            // Verify bidirectional link
            assertEquals(debit.getId(), credit.getLinkedTransactionId(), 
                        "Credit should link back to debit");
        }
    }

    /**
     * Test: Verify TRANSFER category is auto-created on first transfer
     * 
     * Scenario: User creates first transfer when no TRANSFER category exists
     * Expected: System automatically creates TRANSFER category for the user
     */
    @Test
    public void testTransferCategoryAutoCreatedViaAPI() throws Exception {
        // GIVEN: No TRANSFER category exists yet
        assertTrue(categoryRepository.findByUserIdAndName(testUser.getId(), "TRANSFER").isEmpty(),
                  "TRANSFER category should not exist initially");

        // WHEN: Create first transfer
        Map<String, Object> request = new HashMap<>();
        request.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        request.put("toAccountId", destinationAccount.getId());
        request.put("originalAmount", 300.0);
        request.put("originalCurrency", "INR");
        request.put("name", "Category Test Transfer");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // THEN: TRANSFER category should be automatically created
        assertFalse(categoryRepository.findByUserIdAndName(testUser.getId(), "TRANSFER").isEmpty(),
                   "TRANSFER category should be auto-created by the system");
    }

    /**
     * Test: Attempt to update a transfer to have same from/to accounts
     * 
     * Scenario: User creates a valid transfer, then tries to update it to have same from/to accounts
     * Expected: System prevents this invalid state
     */
    @Test
    public void testUpdateTransferToSameAccount_ShouldFail() throws Exception {
        // GIVEN: Create a valid transfer first
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        transferRequest.put("toAccountId", destinationAccount.getId());
        transferRequest.put("originalAmount", 500.0);
        transferRequest.put("originalCurrency", "INR");
        transferRequest.put("name", "Valid Transfer");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        // Get the created debit transaction
        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();

        // WHEN: Try to create another transfer with same from/to accounts
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("accountId", sourceAccount.getId());
        invalidRequest.put("toAccountId", sourceAccount.getId());  // Same as from - INVALID!
        invalidRequest.put("originalAmount", 300.0);
        invalidRequest.put("originalCurrency", "INR");
        invalidRequest.put("name", "Invalid Update");

        // THEN: Should be rejected
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // VERIFY: Original transfer still exists unchanged
        List<Transaction> afterAttempt = transactionRepository.findAll();
        assertEquals(2, afterAttempt.size(), "Should still have only the original 2 transactions");
    }

    /**
     * Test: Comprehensive validation for same account scenarios
     * 
     * Scenario: Various attempts to create transfers with same from/to accounts
     * Expected: All attempts should be rejected with clear error messages
     */
    @Test
    public void testSameAccountValidation_Comprehensive() throws Exception {
        // Test 1: Using accountId and toAccountId (both same)
        Map<String, Object> request1 = new HashMap<>();
        request1.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        request1.put("toAccountId", sourceAccount.getId());
        request1.put("amount", 100.0);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // Test 2: Using accountId and toAccountId (both same)
        Map<String, Object> request2 = new HashMap<>();
        request2.put("accountId", sourceAccount.getId());
        request2.put("toAccountId", sourceAccount.getId());
        request2.put("amount", 200.0);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // Test 3: Using destinationAccount as both from and to
        Map<String, Object> request3 = new HashMap<>();
        request3.put("accountId", destinationAccount.getId());  // REQUIRED @NotNull field
        request3.put("toAccountId", destinationAccount.getId());
        request3.put("amount", 300.0);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // VERIFY: No transactions were created from any of these attempts
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertEquals(0, allTransactions.size(), "No transactions should have been created");
    }

    /**
     * Test: Complete transfer lifecycle from creation to deletion
     * 
     * Scenario: Full end-to-end workflow
     * 1. Create transfer via API
     * 2. Verify transfer exists in database with correct properties
     * 3. Delete transfer via API
     * 4. Verify complete cleanup
     * 
     * Expected: All operations succeed, no data left behind
     */
    @Test
    public void testCompleteTransferLifecycle() throws Exception {
        // STEP 1: Create transfer via API
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 1500.0);
        createRequest.put("originalCurrency", "INR");
        createRequest.put("name", "Lifecycle Test Transfer");
        createRequest.put("comments", "Full lifecycle test");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())  // Should succeed
                .andExpect(jsonPath("$.message", containsString("successfully")));

        // STEP 2: Verify transfer exists in database
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(2, transactions.size(), "Should have 2 transactions after creation");

        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();

        // Verify all transfer properties are correct
        assertEquals(1500.0, debit.getAmount(), "Amount should match");
        assertEquals("Lifecycle Test Transfer", debit.getName(), "Name should match");
        assertEquals("Full lifecycle test", debit.getComments(), "Comments should match");

        // STEP 3: Delete transfer via API
        mockMvc.perform(delete("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())  // Should succeed
                .andExpect(jsonPath("$.message", containsString("Transfer deleted")));

        // STEP 4: Verify complete cleanup - no transactions left
        List<Transaction> afterDelete = transactionRepository.findAll();
        assertEquals(0, afterDelete.size(), "All transactions should be deleted");
    }

    // ============ UPDATE TRANSFER TESTS ============

    /**
     * Test: Update transfer amount via API
     * 
     * Scenario: User creates a transfer and then updates the amount
     * Expected: Both debit and credit sides updated atomically with new amount
     */
    @Test
    public void testUpdateTransferAmount_Success() throws Exception {
        // GIVEN: Create a transfer of $500 from source to destination
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 500.0);  // Initial amount
        createRequest.put("originalCurrency", "INR");
        createRequest.put("name", "Original Transfer");

        // Call POST endpoint to create the transfer
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // Get the debit transaction (money out side) from database
        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)  // TYPE_DEBIT = 1
                .findFirst()
                .orElseThrow();
        
        // Get the linked credit transaction ID for later verification
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update the transfer amount from $500 to $1000 via PUT endpoint
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("accountId", debit.getAccountId());  // REQUIRED @NotNull
        updateRequest.put("originalAmount", 1000.0);  // NEW AMOUNT: Change from 500 to 1000
        updateRequest.put("originalCurrency", debit.getOriginalCurrency());
        updateRequest.put("name", debit.getName());  // Keep same name
        updateRequest.put("comments", debit.getComments());  // Keep same comments
        updateRequest.put("date", debit.getDate());  // Keep same date
        updateRequest.put("isCountable", debit.getIsCountable());  // Keep same countable status

        // Call PUT endpoint to update the transfer
        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transfer updated successfully")));

        // THEN: Verify both debit AND credit sides are updated with new amount
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();

        // Both sides should now have $1000 (atomic update)
        assertEquals(1000.0, updatedDebit.getAmount(), "Debit amount should be updated to 1000");
        assertEquals(1000.0, updatedCredit.getAmount(), "Credit amount should be updated to 1000");
    }

    /**
     * Test: Update transfer name and comments via API
     * 
     * Scenario: User updates the descriptive fields of a transfer
     * Expected: Both sides updated with new name and comments
     */
    @Test
    public void testUpdateTransferNameAndComments_Success() throws Exception {
        // GIVEN: Create a transfer with initial name and comments
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 750.0);
        createRequest.put("originalCurrency", "INR");
        createRequest.put("name", "Old Name");  // Initial name
        createRequest.put("comments", "Old Comment");  // Initial comments

        // Create the transfer via POST endpoint
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // Retrieve the created debit transaction from database
        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)  // TYPE_DEBIT = 1
                .findFirst()
                .orElseThrow();
        
        // Store the credit ID for later verification
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update name and comments (keeping amount and account same)
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("accountId", debit.getAccountId());  // REQUIRED @NotNull - Keep same account
        updateRequest.put("amount", debit.getAmount());  // Keep same amount
        updateRequest.put("name", "Updated Name");  // NEW NAME: Change from "Old Name"
        updateRequest.put("comments", "Updated Comment");  // NEW COMMENTS: Change from "Old Comment"
        updateRequest.put("date", debit.getDate());  // Keep same date
        updateRequest.put("isCountable", debit.getIsCountable());  // Keep same countable status

        // Call PUT endpoint to update the transfer
        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transfer updated successfully")));

        // THEN: Verify BOTH debit and credit have the updated name and comments
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();

        // Verify both sides have new name
        assertEquals("Updated Name", updatedDebit.getName(), "Debit name should be updated");
        assertEquals("Updated Name", updatedCredit.getName(), "Credit name should be updated");
        
        // Verify both sides have new comments
        assertEquals("Updated Comment", updatedDebit.getComments(), "Debit comments should be updated");
        assertEquals("Updated Comment", updatedCredit.getComments(), "Credit comments should be updated");
    }

    /**
     * Test: Update transfer via credit side
     * 
     * Scenario: User updates transfer by calling PUT on the credit transaction
     * Expected: Both sides updated (symmetric behavior)
     */
    @Test
    public void testUpdateTransferViaCreditSide_Success() throws Exception {
        // GIVEN: Create a transfer
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 300.0);
        createRequest.put("originalCurrency", "USD");
        createRequest.put("exchangeRate", 1.0);
        createRequest.put("name", "Credit Side Update Test");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction credit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CREDIT)  // Get credit side
                .findFirst()
                .orElseThrow();
        Long debitId = credit.getLinkedTransactionId();

        // WHEN: Update via CREDIT side (not debit)
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("accountId", credit.getAccountId());  // REQUIRED @NotNull
        // For transfers, controller uses originalAmount/originalCurrency/exchangeRate to compute amount
        updateRequest.put("originalAmount", 600.0);  // Change amount
        updateRequest.put("originalCurrency", "USD");
        updateRequest.put("exchangeRate", 1.0);
        updateRequest.put("name", "Updated via Credit");
        updateRequest.put("date", credit.getDate());  // Keep same date
        updateRequest.put("isCountable", credit.getIsCountable());  // Keep same countable status

        mockMvc.perform(put("/api/transactions/" + credit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transfer updated successfully")));

        // THEN: Both sides should be updated
        Transaction updatedCredit = transactionRepository.findById(credit.getId()).orElseThrow();
        Transaction updatedDebit = transactionRepository.findById(debitId).orElseThrow();

        assertEquals(600.0, updatedCredit.getAmount(), "Credit amount should be updated");
        assertEquals(600.0, updatedDebit.getAmount(), "Debit amount should also be updated");
        assertEquals("Updated via Credit", updatedCredit.getName());
        assertEquals("Updated via Credit", updatedDebit.getName());
    }

    /**
     * Test: Update transfer to change source account
     * 
     * Scenario: User updates transfer to move money from a different account
     * Expected: Debit side account updated, credit side unchanged, both still linked
     */
    @Test
    public void testUpdateTransferChangeSourceAccount_Success() throws Exception {
        // GIVEN: Create third account for testing account changes
        Account thirdAccount = new Account();
        thirdAccount.setName("Third Account");
        thirdAccount.setUserId(testUser.getId());
        thirdAccount.setCurrency("USD");
        thirdAccount = accountRepository.save(thirdAccount);

        // Create initial transfer from source to destination
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 400.0);
        createRequest.put("originalCurrency", "USD");
        createRequest.put("exchangeRate", 1.0);
        createRequest.put("name", "Account Change Test");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update to use third account as source
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("accountId", thirdAccount.getId());  // Change from source to third - REQUIRED @NotNull
        updateRequest.put("amount", debit.getAmount());
        updateRequest.put("name", debit.getName());
        updateRequest.put("date", debit.getDate());  // Keep same date
        updateRequest.put("isCountable", debit.getIsCountable());  // Keep same countable status

        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // THEN: Debit account changed, credit unchanged
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();

        assertEquals(thirdAccount.getId(), updatedDebit.getAccountId(), 
                    "Debit should be on new source account");
        assertEquals(destinationAccount.getId(), updatedCredit.getAccountId(), 
                    "Credit should still be on original destination");
        // Verify still linked
        assertEquals(creditId, updatedDebit.getLinkedTransactionId());
        assertEquals(debit.getId(), updatedCredit.getLinkedTransactionId());
    }

    /**
     * Test: Attempt to update transfer to make source and destination the same
     * 
     * Scenario: User tries to update a valid transfer to have same from/to accounts
     * Expected: API rejects with 400 Bad Request
     */
    @Test
    public void testUpdateTransferToSameFromToAccount_ShouldFail() throws Exception {
        // GIVEN: Create a valid transfer
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 500.0);
        createRequest.put("originalCurrency", "USD");
        createRequest.put("exchangeRate", 1.0);
        createRequest.put("name", "Valid Transfer");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();

        // WHEN: Try to update to make both accounts the same
        Map<String, Object> invalidUpdate = new HashMap<>();
        invalidUpdate.put("accountId", sourceAccount.getId());  // Same as from - REQUIRED @NotNull
        invalidUpdate.put("amount", 500.0);
        invalidUpdate.put("name", "Invalid Update");
        invalidUpdate.put("date", new java.util.Date());  // Add date
        invalidUpdate.put("isCountable", 0);  // Add countable status

        // Get the credit transaction to try changing its account
        Transaction credit = transactionRepository.findById(debit.getLinkedTransactionId()).orElseThrow();
        
        // Try updating credit to use same account as debit
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("accountId", sourceAccount.getId());  // Try to make it same as debit - REQUIRED @NotNull
        // Provide original fields though validation should fail on same-account check first
        creditUpdate.put("originalAmount", credit.getOriginalAmount());
        creditUpdate.put("originalCurrency", credit.getOriginalCurrency());
        creditUpdate.put("exchangeRate", credit.getExchangeRate());
        creditUpdate.put("name", credit.getName());
        creditUpdate.put("date", credit.getDate());  // Keep same date
        creditUpdate.put("isCountable", credit.getIsCountable());  // Keep same countable status

        // THEN: Should be rejected
        mockMvc.perform(put("/api/transactions/" + credit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // VERIFY: Original transfer unchanged
        Transaction unchangedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction unchangedCredit = transactionRepository.findById(credit.getId()).orElseThrow();
        
        assertEquals(sourceAccount.getId(), unchangedDebit.getAccountId());
        assertEquals(destinationAccount.getId(), unchangedCredit.getAccountId());
    }

    /**
     * Test: Update regular (non-transfer) transaction should work normally
     * 
     * Scenario: User updates a regular transaction (not part of a transfer)
     * Expected: Only that transaction updated, no transfer logic triggered
     */
    @Test
    public void testUpdateRegularTransaction_NotAffectingTransferLogic() throws Exception {
        // GIVEN: Create a regular transaction (NOT a transfer)
        Transaction regular = new Transaction();
        regular.setAccountId(sourceAccount.getId());
        regular.setCategoryId(testCategory.getId());
        regular.setTransactionType(TransactionType.DEBIT);  // DEBIT
        regular.setOriginalAmount(100.0);
        regular.setOriginalCurrency("INR");
        regular.setExchangeRate(1.0);
        regular.setDate(new java.util.Date());
        regular.setIsCountable(1);
        regular.setName("Regular Transaction");
        regular = transactionRepository.save(regular);

        // WHEN: Update the regular transaction (amount derived from originalAmount * exchangeRate)
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("accountId", regular.getAccountId());  // REQUIRED @NotNull
        updateRequest.put("categoryId", regular.getCategoryId());  // Required for regular transactions
        updateRequest.put("transactionType", regular.getTransactionType());  // Required for regular transactions
        updateRequest.put("originalAmount", 200.0);  // Change amount via source fields
        updateRequest.put("originalCurrency", "INR");
        updateRequest.put("exchangeRate", 1.0);
        updateRequest.put("name", "Updated Regular");
        updateRequest.put("date", regular.getDate());  // Keep same date
        updateRequest.put("isCountable", 1);

        mockMvc.perform(put("/api/transactions/" + regular.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transaction updated successfully")));  // Not "Transfer"

        // THEN: Only this transaction updated
        Transaction updated = transactionRepository.findById(regular.getId()).orElseThrow();
        assertEquals(200.0, updated.getAmount(), "Amount should be updated");
        assertEquals("Updated Regular", updated.getName(), "Name should be updated");
        assertNull(updated.getLinkedTransactionId(), "Should not have linked transaction");

        // Verify no other transactions created
        List<Transaction> all = transactionRepository.findAll();
        assertEquals(1, all.size(), "Should still have only 1 transaction");
    }

    /**
     * Test: Update transfer multiple times in sequence
     * 
     * Scenario: User updates the same transfer several times
     * Expected: Each update succeeds, final state reflects last update
     */
    @Test
    public void testMultipleSequentialUpdates_Success() throws Exception {
        // GIVEN: Create a transfer
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 100.0);
        createRequest.put("originalCurrency", "USD");
        createRequest.put("exchangeRate", 1.0);
        createRequest.put("name", "Version 1");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update multiple times
        for (int i = 2; i <= 5; i++) {
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("accountId", debit.getAccountId());  // REQUIRED @NotNull
            updateRequest.put("originalAmount", 100.0 * i);  // Increase amount each time via original fields
            updateRequest.put("originalCurrency", "USD");
            updateRequest.put("exchangeRate", 1.0);
            updateRequest.put("name", "Version " + i);
            updateRequest.put("date", debit.getDate());  // Keep same date
            updateRequest.put("isCountable", debit.getIsCountable());  // Keep same countable status

            mockMvc.perform(put("/api/transactions/" + debit.getId())
                            .header("Authorization", bearerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());
        }

        // THEN: Final state should reflect last update
        Transaction finalDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction finalCredit = transactionRepository.findById(creditId).orElseThrow();

        assertEquals(500.0, finalDebit.getAmount(), "Should have final amount");
        assertEquals(500.0, finalCredit.getAmount(), "Should have final amount");
        assertEquals("Version 5", finalDebit.getName(), "Should have final name");
        assertEquals("Version 5", finalCredit.getName(), "Should have final name");

        // Still only 2 transactions (no duplicates created)
        assertEquals(2, transactionRepository.findAll().size());
    }

    /**
     * Test: Complete CRUD lifecycle including update
     * 
     * Scenario: Create -> Update -> Delete
     * Expected: All operations work correctly in sequence
     */
    @Test
    public void testCompleteCRUDLifecycleWithUpdate() throws Exception {
        // CREATE
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("accountId", sourceAccount.getId());  // REQUIRED @NotNull field
        createRequest.put("toAccountId", destinationAccount.getId());
        createRequest.put("originalAmount", 100.0);
        createRequest.put("originalCurrency", "USD");
        createRequest.put("exchangeRate", 83.5);
        createRequest.put("name", "CRUD Test");
        createRequest.put("comments", "CRUD lifecycle test");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> afterCreate = transactionRepository.findAll();
        assertEquals(2, afterCreate.size());

        Transaction debit = afterCreate.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();

        // UPDATE
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("accountId", debit.getAccountId());  // REQUIRED @NotNull
        // For transfers, controller uses originalAmount/originalCurrency/exchangeRate to compute amount
        updateRequest.put("originalAmount", 2000.0);
        updateRequest.put("originalCurrency", "USD");
        updateRequest.put("exchangeRate", 1.0);
        updateRequest.put("name", "CRUD Test Updated");
        updateRequest.put("date", debit.getDate());  // Keep same date
        updateRequest.put("isCountable", debit.getIsCountable());  // Keep same countable status

        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        Transaction afterUpdate = transactionRepository.findById(debit.getId()).orElseThrow();
        assertEquals(2000.0, afterUpdate.getAmount());
        assertEquals("CRUD Test Updated", afterUpdate.getName());

        // DELETE
        mockMvc.perform(delete("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        List<Transaction> afterDelete = transactionRepository.findAll();
        assertEquals(0, afterDelete.size());
    }
}
