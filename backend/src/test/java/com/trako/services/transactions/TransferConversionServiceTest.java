package com.trako.services.transactions;

import com.trako.dtos.TransferResult;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionEntryType;
import com.trako.entities.TransactionType;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferConversionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidationService validationService;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferConversionService transferConversionService;

    private static final String USER_ID = "user123";
    private static final Long TRANSACTION_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long TO_ACCOUNT_ID = 20L;
    private static final Long LINKED_TRANSACTION_ID = 2L;
    private static final Long TRANSFER_CATEGORY_ID = 100L;

    private Transaction existingTransaction;
    private Transaction linkedTransaction;
    private TransactionRequest transferRequest;
    private TransactionRequest regularRequest;

    @BeforeEach
    public void setup() {
        // Setup existing regular transaction (expense)
        existingTransaction = new Transaction();
        existingTransaction.setId(TRANSACTION_ID);
        existingTransaction.setAccountId(ACCOUNT_ID);
        existingTransaction.setCategoryId(5L);
        existingTransaction.setTransactionType(TransactionEntryType.DEBIT);
        existingTransaction.setOriginalAmount(100.0);
        existingTransaction.setOriginalCurrency("USD");
        existingTransaction.setExchangeRate(1.0);
        existingTransaction.setLinkedTransactionId(null);
        existingTransaction.setIsCountable(1);
        existingTransaction.setName("Grocery");
        existingTransaction.setDate(new Date());

        // Setup transfer request
        transferRequest = new TransactionRequest(
            null,           // id
            ACCOUNT_ID,     // accountId
            new Date(),     // date
            "Transfer",     // name
            "Test transfer", // comments
            null,           // categoryId
            null,           // transactionType
            null,           // isCountable
            "USD",          // originalCurrency
            100.0,          // originalAmount
            1.0,            // exchangeRate
            null,           // linkedTransactionId
            TO_ACCOUNT_ID,  // toAccountId
            null            // fromAccountId
        );

        // Setup regular conversion request
        regularRequest = new TransactionRequest(
            null,           // id
            null,           // accountId
            null,           // date
            "Regular",      // name
            null,           // comments
            5L,             // categoryId - new category
            TransactionType.DEBIT, // transactionType
            1,              // isCountable
            null,           // originalCurrency
            null,           // originalAmount
            null,           // exchangeRate
            null,           // linkedTransactionId
            null,           // toAccountId
            null            // fromAccountId
        );

        // Setup linked transaction for transfer
        linkedTransaction = new Transaction();
        linkedTransaction.setId(LINKED_TRANSACTION_ID);
        linkedTransaction.setAccountId(TO_ACCOUNT_ID);
        linkedTransaction.setTransactionType(TransactionEntryType.CREDIT);
        linkedTransaction.setLinkedTransactionId(TRANSACTION_ID);
    }

    // ==================== convertRegularToTransfer Tests ====================

    @Test
    public void testConvertRegularToTransfer_Success() {
        // Given
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));
        when(transferService.getOrCreateTransferCategory(USER_ID)).thenReturn(TRANSFER_CATEGORY_ID);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction t = invocation.getArgument(0);
                if (t.getId() == null) {
                    t.setId(LINKED_TRANSACTION_ID);
                }
                return t;
            });
        when(transactionRepository.findById(LINKED_TRANSACTION_ID)).thenReturn(Optional.of(linkedTransaction));

        // When
        TransferResult result = transferConversionService.convertRegularToTransfer(USER_ID, TRANSACTION_ID, transferRequest);

        // Then

        // Verify debit side
        Transaction debit = result.debit();
        assertThat(debit.getId()).isEqualTo(TRANSACTION_ID);
        assertThat(debit.getTransactionType()).isEqualTo(TransactionEntryType.DEBIT);
        assertThat(debit.getCategoryId()).isEqualTo(TRANSFER_CATEGORY_ID);
        assertThat(debit.getIsCountable()).isEqualTo(0);
        assertThat(debit.getLinkedTransactionId()).isEqualTo(LINKED_TRANSACTION_ID);

        // Verify credit side
        Transaction credit = result.credit();
        assertThat(credit.getId()).isEqualTo(LINKED_TRANSACTION_ID);
        assertThat(credit.getTransactionType()).isEqualTo(TransactionEntryType.CREDIT);
        assertThat(credit.getAccountId()).isEqualTo(TO_ACCOUNT_ID);
        assertThat(credit.getLinkedTransactionId()).isEqualTo(TRANSACTION_ID);

        verify(validationService, times(2)).validateAccountOwnership(USER_ID, ACCOUNT_ID);
        verify(validationService, times(3)).validateAccountOwnership(USER_ID, TO_ACCOUNT_ID);
        verify(transactionRepository, times(3)).saveAndFlush(any(Transaction.class));
    }

    @Test
    public void testConvertRegularToTransfer_AlreadyTransfer_ThrowsException() {
        // Given - transaction already has a linked transaction
        existingTransaction.setLinkedTransactionId(LINKED_TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));

        // When/Then
        assertThatThrownBy(() -> transferConversionService.convertRegularToTransfer(USER_ID, TRANSACTION_ID, transferRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction is already a transfer");
    }

    @Test
    public void testConvertRegularToTransfer_TransactionNotFound_ThrowsException() {
        // Given
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> transferConversionService.convertRegularToTransfer(USER_ID, TRANSACTION_ID, transferRequest))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Transaction not found: " + TRANSACTION_ID);
    }

    @Test
    public void testConvertRegularToTransfer_SameAccount_ThrowsException() {
        // Given - toAccountId is same as from account
        TransactionRequest sameAccountRequest = new TransactionRequest(
            null, ACCOUNT_ID, new Date(), "Transfer", null,
            null, null, null, "USD", 100.0, 1.0, null,
            ACCOUNT_ID, null  // toAccountId same as accountId
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));

        // When/Then
        assertThatThrownBy(() -> transferConversionService.convertRegularToTransfer(USER_ID, TRANSACTION_ID, sameAccountRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Source and destination accounts cannot be the same");
    }

    @Test
    public void testConvertRegularToTransfer_AppliesRequestOverrides() {
        // Given - request with overrides
        Date newDate = new Date(System.currentTimeMillis() + 86400000);
        TransactionRequest overrideRequest = new TransactionRequest(
            null, ACCOUNT_ID, newDate, "New Name", "New Comments",
            null, null, null, "EUR", 200.0, 0.85, null,
            TO_ACCOUNT_ID, null
        );

        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));
        when(transferService.getOrCreateTransferCategory(USER_ID)).thenReturn(TRANSFER_CATEGORY_ID);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction t = invocation.getArgument(0);
                if (t.getId() == null) {
                    t.setId(LINKED_TRANSACTION_ID);
                }
                return t;
            });
        when(transactionRepository.findById(LINKED_TRANSACTION_ID)).thenReturn(Optional.of(linkedTransaction));

        // When
        TransferResult result = transferConversionService.convertRegularToTransfer(USER_ID, TRANSACTION_ID, overrideRequest);

        // Then
        Transaction debit = result.debit();
        assertThat(debit.getDate()).isEqualTo(newDate);
        assertThat(debit.getName()).isEqualTo("New Name");
        assertThat(debit.getComments()).isEqualTo("New Comments");
        assertThat(debit.getOriginalAmount()).isEqualTo(200.0);
        assertThat(debit.getOriginalCurrency()).isEqualTo("EUR");
        assertThat(debit.getExchangeRate()).isEqualTo(0.85);
    }

    // ==================== convertTransferToRegular Tests ====================

    @Test
    public void testConvertTransferToRegular_Success() {
        // Given
        existingTransaction.setLinkedTransactionId(LINKED_TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.existsById(LINKED_TRANSACTION_ID)).thenReturn(true);
        when(validationService.validateTransactionOwnership(USER_ID, LINKED_TRANSACTION_ID)).thenReturn(linkedTransaction);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Transaction result = transferConversionService.convertTransferToRegular(USER_ID, TRANSACTION_ID, regularRequest);

        // Then
        assertThat(result.getLinkedTransactionId()).isNull();
        assertThat(result.getCategoryId()).isEqualTo(5L);
        assertThat(result.getTransactionType()).isEqualTo(TransactionEntryType.DEBIT);
        assertThat(result.getIsCountable()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Regular");

        verify(transactionRepository).delete(linkedTransaction);
    }

    @Test
    public void testConvertTransferToRegular_DefaultsToCountable() {
        // Given - request without isCountable
        TransactionRequest noCountableRequest = new TransactionRequest(
            null, null, null, null, null,
            5L, TransactionType.DEBIT, null,  // isCountable = null
            null, null, null, null, null, null
        );

        existingTransaction.setLinkedTransactionId(LINKED_TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.existsById(LINKED_TRANSACTION_ID)).thenReturn(true);
        when(validationService.validateTransactionOwnership(USER_ID, LINKED_TRANSACTION_ID)).thenReturn(linkedTransaction);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Transaction result = transferConversionService.convertTransferToRegular(USER_ID, TRANSACTION_ID, noCountableRequest);

        // Then
        assertThat(result.getIsCountable()).isEqualTo(1);
    }

    @Test
    public void testConvertTransferToRegular_NotATransfer_ThrowsException() {
        // Given - transaction has no linked transaction
        existingTransaction.setLinkedTransactionId(null);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));

        // When/Then
        assertThatThrownBy(() -> transferConversionService.convertTransferToRegular(USER_ID, TRANSACTION_ID, regularRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction is not a transfer");
    }

    @Test
    public void testConvertTransferToRegular_LinkedTransactionNotFound_ThrowsException() {
        // Given
        existingTransaction.setLinkedTransactionId(LINKED_TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.existsById(LINKED_TRANSACTION_ID)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> transferConversionService.convertTransferToRegular(USER_ID, TRANSACTION_ID, regularRequest))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Linked transaction not found: " + LINKED_TRANSACTION_ID);
    }

    @Test
    public void testConvertTransferToRegular_TransactionNotFound_ThrowsException() {
        // Given
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> transferConversionService.convertTransferToRegular(USER_ID, TRANSACTION_ID, regularRequest))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Transaction not found: " + TRANSACTION_ID);
    }

    @Test
    public void testConvertTransferToRegular_PreservesExistingValues() {
        // Given - request with minimal fields
        TransactionRequest minimalRequest = new TransactionRequest(
            null, null, null, null, null,
            null, null, null,  // no category, type, or countable
            null, null, null, null, null, null
        );

        existingTransaction.setLinkedTransactionId(LINKED_TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.existsById(LINKED_TRANSACTION_ID)).thenReturn(true);
        when(validationService.validateTransactionOwnership(USER_ID, LINKED_TRANSACTION_ID)).thenReturn(linkedTransaction);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Transaction result = transferConversionService.convertTransferToRegular(USER_ID, TRANSACTION_ID, minimalRequest);

        // Then - should preserve existing values since request has nulls
        assertThat(result.getCategoryId()).isEqualTo(5L);  // original category
        assertThat(result.getTransactionType()).isEqualTo(TransactionEntryType.DEBIT);  // original type
        assertThat(result.getIsCountable()).isEqualTo(1);  // default
        assertThat(result.getName()).isEqualTo("Grocery");  // original name
    }
}
