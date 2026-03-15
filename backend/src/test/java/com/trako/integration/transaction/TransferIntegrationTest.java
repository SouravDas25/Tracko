package com.trako.integration.transaction;

import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Integration tests for transfer operations.
 * <p>
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
@Transactional
public class TransferIntegrationTest extends BaseIntegrationTest {

    private User testUser;
    private Account sourceAccount;
    private Account destinationAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        // Create test user
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);

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
     * <p>
     * Scenario: User creates a transfer between two accounts
     * Expected: Transfer creates two linked transactions (debit + credit) atomically
     */
    @Test
    public void testCreateTransferViaAPI_Success() throws Exception {
        // GIVEN: Prepare transfer request payload
        TransactionRequest transferRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (source)
                new java.util.Date(),    // date
                "API Test Transfer",     // name
                "Integration test via API", // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                500.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

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
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)  // Find debit (money out)
                .findFirst()
                .orElse(null);
        // VERIFY: Find and validate credit transaction (TYPE_CREDIT = 2)
        Transaction credit = transferTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.CREDIT)  // Find credit (money in)
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
     * <p>
     * Scenario: User tries to transfer money from Account A to Account A
     * Expected: API rejects with 400 Bad Request and appropriate error message
     */
    @Test
    public void testCreateTransferViaAPI_SameAccount_ReturnsBadRequest() throws Exception {
        // GIVEN: Prepare invalid transfer request (same account)
        TransactionRequest transferRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (source)
                new java.util.Date(),    // date
                "Invalid Transfer",      // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                500.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                sourceAccount.getId(),   // toAccountId (same as source -> invalid)
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: User tries to transfer to an account that doesn't exist
     * Expected: API rejects with 400 Bad Request
     */
    @Test
    public void testCreateTransferViaAPI_InvalidAccount_ReturnsBadRequest() throws Exception {
        // GIVEN: Prepare transfer request with non-existent account ID
        TransactionRequest transferRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (source)
                new java.util.Date(),    // date
                "Invalid Transfer",      // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                500.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                99999L,                  // toAccountId (non-existent)
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: User deletes a transfer by calling DELETE on the debit transaction
     * Expected: BOTH debit AND credit transactions are deleted atomically
     */
    @Test
    public void testDeleteTransferViaAPI_Success() throws Exception {
        // GIVEN: Create a transfer via API first
        TransactionRequest transferRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Transfer to Delete",    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                750.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

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
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)  // TYPE_DEBIT
                .findFirst()
                .orElseThrow();

        Long debitId = debit.getId();
        Long creditId = debit.getLinkedTransactionId();  // Get linked credit ID

        // WHEN: Delete via API using debit transaction ID
        mockMvc.perform(delete("/api/transactions/" + debitId)
                        .header("Authorization", bearerToken))
                // THEN: Should return success
                .andExpect(status().isOk())  // 200 OK
                .andExpect(jsonPath("$.message", containsString("Transaction deleted successfully")));

        // VERIFY: BOTH transactions should be deleted from database
        assertFalse(transactionRepository.findById(debitId).isPresent(),
                "Debit transaction should be deleted");
        assertFalse(transactionRepository.findById(creditId).isPresent(),
                "Credit transaction should also be deleted (atomic operation)");
    }

    /**
     * Test: Delete a transfer by deleting the credit side
     * <p>
     * Scenario: User deletes a transfer by calling DELETE on the credit transaction
     * Expected: BOTH debit AND credit transactions are deleted (symmetric behavior)
     */
    @Test
    public void testDeleteTransferViaAPI_ViaCreditSide_Success() throws Exception {
        // GIVEN: Create a transfer via API
        TransactionRequest transferRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Transfer to Delete via Credit", // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                1000.0,                  // originalAmount
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        // Get the created transactions
        List<Transaction> transactions = transactionRepository.findAll();
        // Find the credit transaction (money in)
        Transaction credit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.CREDIT)  // TYPE_CREDIT
                .findFirst()
                .orElseThrow();

        Long creditId = credit.getId();
        Long debitId = credit.getLinkedTransactionId();  // Get linked debit ID

        // WHEN: Delete via credit side (instead of debit)
        mockMvc.perform(delete("/api/transactions/" + creditId)
                        .header("Authorization", bearerToken))
                // THEN: Should still delete both transactions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transaction deleted successfully")));

        // VERIFY: Both transactions deleted (symmetric behavior)
        assertFalse(transactionRepository.findById(debitId).isPresent(),
                "Debit should be deleted");
        assertFalse(transactionRepository.findById(creditId).isPresent(),
                "Credit should be deleted");
    }

    /**
     * Test: Delete a regular (non-transfer) transaction
     * <p>
     * Scenario: User deletes a normal transaction (not part of a transfer)
     * Expected: Only the single transaction is deleted, no special transfer handling
     */
    @Test
    public void testDeleteRegularTransactionViaAPI_NotAffectingTransfers() throws Exception {
        // GIVEN: Create a regular transaction (NOT a transfer)
        Transaction regular = new Transaction();
        regular.setAccountId(sourceAccount.getId());
        regular.setCategoryId(testCategory.getId());  // Use created test category
        regular.setTransactionType(TransactionDbType.DEBIT);  // TYPE_DEBIT
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
     * <p>
     * Scenario: User creates 3 separate transfers in sequence
     * Expected: Each transfer is independent, properly linked, no cross-linking
     */
    @Test
    public void testMultipleTransfersViaAPI_AllIndependent() throws Exception {
        // GIVEN & WHEN: Create 3 transfers with different amounts
        for (int i = 1; i <= 3; i++) {
            TransactionRequest request = new TransactionRequest(
                    null,                    // id
                    sourceAccount.getId(),   // accountId
                    new java.util.Date(),    // date
                    "Transfer " + i,         // name
                    null,                    // comments
                    null,                    // categoryId
                    TransactionType.TRANSFER,// transactionType
                    "INR",                   // originalCurrency
                    100.0 * i,               // originalAmount
                    null,                    // exchangeRate
                    null,                    // linkedTransactionId
                    destinationAccount.getId(), // toAccountId
                    null                     // fromAccountId
            );

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
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)  // Get all debits
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
     * <p>
     * Scenario: User creates first transfer when no TRANSFER category exists
     * Expected: System automatically creates TRANSFER category for the user
     */
    @Test
    public void testTransferCategoryAutoCreatedViaAPI() throws Exception {
        // GIVEN: No TRANSFER category exists yet
        assertTrue(categoryRepository.findByUserIdAndName(testUser.getId(), "TRANSFER").isEmpty(),
                "TRANSFER category should not exist initially");

        // WHEN: Create first transfer
        TransactionRequest request = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Category Test Transfer", // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                300.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: User creates a valid transfer, then tries to update it to have same from/to accounts
     * Expected: System prevents this invalid state
     */
    @Test
    public void testUpdateTransferToSameAccount_ShouldFail() throws Exception {
        // GIVEN: Create a valid transfer first
        TransactionRequest transferRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Valid Transfer",        // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                500.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        // Get the created debit transaction
        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
                .findFirst()
                .orElseThrow();

        // WHEN: Try to create another transfer with same from/to accounts
        TransactionRequest invalidRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Invalid Update",        // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                300.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                sourceAccount.getId(),   // toAccountId (same as source)
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: Various attempts to create transfers with same from/to accounts
     * Expected: All attempts should be rejected with clear error messages
     */
    @Test
    public void testSameAccountValidation_Comprehensive() throws Exception {
        // Test 1: Using accountId and toAccountId (both same)
        TransactionRequest request1 = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                100.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                sourceAccount.getId(),   // toAccountId (same)
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // Test 2: Using accountId and toAccountId (both same)
        TransactionRequest request2 = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                200.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                sourceAccount.getId(),   // toAccountId (same)
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same")));

        // Test 3: Using destinationAccount as both from and to
        TransactionRequest request3 = new TransactionRequest(
                null,                    // id
                destinationAccount.getId(), // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                300.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId (same)
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: Full end-to-end workflow
     * 1. Create transfer via API
     * 2. Verify transfer exists in database with correct properties
     * 3. Delete transfer via API
     * 4. Verify complete cleanup
     * <p>
     * Expected: All operations succeed, no data left behind
     */
    @Test
    public void testCompleteTransferLifecycle() throws Exception {
        // STEP 1: Create transfer via API
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Lifecycle Test Transfer", // name
                "Full lifecycle test",   // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                1500.0,                  // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

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
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
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
                .andExpect(jsonPath("$.message", containsString("Transaction deleted successfully")));

        // STEP 4: Verify complete cleanup - no transactions left
        List<Transaction> afterDelete = transactionRepository.findAll();
        assertEquals(0, afterDelete.size(), "All transactions should be deleted");
    }

    // ============ UPDATE TRANSFER TESTS ============

    /**
     * Test: Update transfer amount via API
     * <p>
     * Scenario: User creates a transfer and then updates the amount
     * Expected: Both debit and credit sides updated atomically with new amount
     */
    @Test
    public void testUpdateTransferAmount_Success() throws Exception {
        // GIVEN: Create a transfer of $500 from source to destination
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Original Transfer",     // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                500.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        // Call POST endpoint to create the transfer
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // Get the debit transaction (money out side) from database
        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)  // TYPE_DEBIT = 1
                .findFirst()
                .orElseThrow();

        // Get the linked credit transaction ID for later verification
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update the transfer amount from $500 to $1000 via PUT endpoint
        TransactionRequest updateRequest = new TransactionRequest(
                null,                    // id
                debit.getAccountId(),    // accountId
                debit.getDate(),         // date
                debit.getName(),         // name
                debit.getComments(),     // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                debit.getOriginalCurrency(), // originalCurrency
                1000.0,                  // originalAmount (new)
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        // Call PUT endpoint to update the transfer
        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transaction updated successfully")));

        // THEN: Verify both debit AND credit sides are updated with new amount
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();

        // Both sides should now have $1000 (atomic update)
        assertEquals(1000.0, updatedDebit.getAmount(), "Debit amount should be updated to 1000");
        assertEquals(1000.0, updatedCredit.getAmount(), "Credit amount should be updated to 1000");
    }

    /**
     * Test: Update transfer name and comments via API
     * <p>
     * Scenario: User updates the descriptive fields of a transfer
     * Expected: Both sides updated with new name and comments
     */
    @Test
    public void testUpdateTransferNameAndComments_Success() throws Exception {
        // GIVEN: Create a transfer with initial name and comments
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Initial Transfer",      // name
                "Initial Comments",      // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                750.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // Retrieve the created debit transaction from database
        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)  // TYPE_DEBIT = 1
                .findFirst()
                .orElseThrow();

        // Store the credit ID for later verification
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update name and comments (keeping amount and account same)
        TransactionRequest updateRequest = new TransactionRequest(
                null,                    // id
                debit.getAccountId(),    // accountId
                debit.getDate(),         // date
                "Updated Name",          // name
                "Updated Comments",      // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // originalCurrency
                null,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        // Call PUT endpoint to update the transfer
        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transaction updated successfully")));

        // THEN: Verify BOTH debit and credit have the updated name and comments
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(creditId).orElseThrow();

        // Verify both sides have new name
        assertEquals("Updated Name", updatedDebit.getName(), "Debit name should be updated");
        assertEquals("Updated Name", updatedCredit.getName(), "Credit name should be updated");

        // Verify both sides have new comments
        assertEquals("Updated Comments", updatedDebit.getComments(), "Debit comments should be updated");
        assertEquals("Updated Comments", updatedCredit.getComments(), "Credit comments should be updated");
    }

    /**
     * Test: Update transfer via credit side
     * <p>
     * Scenario: User updates transfer by calling PUT on the credit transaction
     * Expected: Both sides updated (symmetric behavior)
     */
    @Test
    public void testUpdateTransferViaCreditSide_Success() throws Exception {
        // GIVEN: Create a transfer
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (source)
                new java.util.Date(),    // date
                "Credit Side Update Test", // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                300.0,                   // originalAmount
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction credit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.CREDIT)  // Get credit side
                .findFirst()
                .orElseThrow();
        Long debitId = credit.getLinkedTransactionId();

        // WHEN: Update via CREDIT side (not debit)
        TransactionRequest updateRequest = new TransactionRequest(
                null,                    // id
                credit.getAccountId(),    // accountId (credit side)
                credit.getDate(),         // date (keep same)
                "Updated via Credit",   // name
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "USD",                   // originalCurrency
                600.0,                   // originalAmount (change to 600.0)
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId (keep existing)
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + credit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Transaction updated successfully")));

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
     * <p>
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
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (source)
                new java.util.Date(),    // date
                "Account Change Test",  // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                400.0,                   // originalAmount
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
                .findFirst()
                .orElseThrow();
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update to use third account as source
        TransactionRequest updateRequest = new TransactionRequest(
                null,                    // id
                thirdAccount.getId(),     // accountId (change to third account)
                debit.getDate(),          // date (keep same)
                debit.getName(),          // name (keep same)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                null,                    // originalCurrency (keep existing)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId (keep existing)
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: User tries to update a valid transfer to have same from/to accounts
     * Expected: API rejects with 400 Bad Request
     */
    @Test
    public void testUpdateTransferToSameFromToAccount_ShouldFail() throws Exception {
        // GIVEN: Create a valid transfer
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (source)
                new java.util.Date(),    // date
                "Valid Transfer",       // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                500.0,                   // originalAmount
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
                .findFirst()
                .orElseThrow();

        // WHEN: Try to update to make both accounts the same
        TransactionRequest invalidUpdate = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (same as from)
                new java.util.Date(),    // date
                "Invalid Update",       // name
                null,                    // comments
                null,                    // categoryId
                null,                    // transactionType (keep existing)
                null,                    // originalCurrency (keep existing)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId (keep existing)
                null                     // fromAccountId
        );

        // Get the credit transaction to try changing its account
        Transaction credit = transactionRepository.findById(debit.getLinkedTransactionId()).orElseThrow();

        // Try updating credit to use same account as debit
        TransactionRequest creditUpdate = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId (try to make it same as debit)
                credit.getDate(),         // date (keep same)
                credit.getName(),         // name (keep same)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                credit.getOriginalCurrency(), // originalCurrency (keep same)
                credit.getOriginalAmount(),   // originalAmount (keep same)
                credit.getExchangeRate(),     // exchangeRate (keep same)
                null,                    // linkedTransactionId
                null,                    // toAccountId (keep existing)
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: User updates a regular transaction (not part of a transfer)
     * Expected: Only that transaction updated, no transfer logic triggered
     */
    @Test
    public void testUpdateRegularTransaction_NotAffectingTransferLogic() throws Exception {
        // GIVEN: Create a regular transaction (NOT a transfer)
        Transaction regular = new Transaction();
        regular.setAccountId(sourceAccount.getId());
        regular.setCategoryId(testCategory.getId());
        regular.setTransactionType(TransactionDbType.DEBIT);  // DEBIT
        regular.setOriginalAmount(100.0);
        regular.setOriginalCurrency("INR");
        regular.setExchangeRate(1.0);
        regular.setDate(new java.util.Date());
        regular.setIsCountable(1);
        regular.setName("Regular Transaction");
        regular = transactionRepository.save(regular);

        // WHEN: Update the regular transaction (amount derived from originalAmount * exchangeRate)
        TransactionRequest updateRequest = new TransactionRequest(
                null,                    // id
                regular.getAccountId(),   // accountId
                regular.getDate(),        // date (keep same)
                "Updated Regular",      // name
                null,                    // comments (keep existing)
                regular.getCategoryId(),  // categoryId (required for regular transactions)
                TransactionType.DEBIT,   // transactionType (from regular.getTransactionType())
                "INR",                   // originalCurrency
                200.0,                   // originalAmount (change to 200.0)
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

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
     * <p>
     * Scenario: User updates the same transfer several times
     * Expected: Each update succeeds, final state reflects last update
     */
    @Test
    public void testMultipleSequentialUpdates_Success() throws Exception {
        // GIVEN: Create a transfer
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "Initial Transfer",      // name
                "Initial Comments",      // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                750.0,                   // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> transactions = transactionRepository.findAll();
        Transaction debit = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
                .findFirst()
                .orElseThrow();
        Long creditId = debit.getLinkedTransactionId();

        // WHEN: Update multiple times
        for (int i = 2; i <= 5; i++) {
            TransactionRequest updateRequest = new TransactionRequest(
                    null,                    // id
                    debit.getAccountId(),    // accountId
                    debit.getDate(),         // date
                    "Version " + i,         // name
                    null,                    // comments
                    null,                    // categoryId
                    TransactionType.TRANSFER,// transactionType
                    "USD",                   // originalCurrency
                    100.0 * i,               // originalAmount
                    1.0,                     // exchangeRate
                    null,                    // linkedTransactionId
                    null,                    // toAccountId
                    null                     // fromAccountId
            );

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
     * <p>
     * Scenario: Create -> Update -> Delete
     * Expected: All operations work correctly in sequence
     */
    @Test
    public void testCompleteCRUDLifecycleWithUpdate() throws Exception {
        // CREATE
        TransactionRequest createRequest = new TransactionRequest(
                null,                    // id
                sourceAccount.getId(),   // accountId
                new java.util.Date(),    // date
                "CRUD Test",             // name
                "CRUD lifecycle test",  // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                100.0,                   // originalAmount
                83.5,                    // exchangeRate
                null,                    // linkedTransactionId
                destinationAccount.getId(), // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        List<Transaction> afterCreate = transactionRepository.findAll();
        assertEquals(2, afterCreate.size());

        Transaction debit = afterCreate.stream()
                .filter(t -> t.getTransactionType() == TransactionDbType.DEBIT)
                .findFirst()
                .orElseThrow();

        // UPDATE
        TransactionRequest updateRequest = new TransactionRequest(
                null,                    // id
                debit.getAccountId(),    // accountId
                debit.getDate(),         // date
                "CRUD Test Updated",    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                2000.0,                  // originalAmount
                1.0,                     // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

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
