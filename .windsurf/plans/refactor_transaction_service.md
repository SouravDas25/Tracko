# Refactoring Plan for Transaction Services

## Goal
Improve maintainability of `TransactionWriteService` and related classes to prevent future errors.

## Identified Issues
1. **God-Class/Method**: `TransactionWriteService` handles too many responsibilities (validation, conversion, persistence).
2. **Duplicated Logic**: Ownership validation is repeated.
3. **Presentation Logic in Controller**: `TransactionController` modifies entities for view purposes (hiding credits, marking transfers).
4. **Hardcoded Strings**: "TRANSFER" category name is hardcoded.
5. **Weak Typing**: `Transaction[]` return type for transfers is error-prone.

## Plan

### 1. Centralize Constants
Create `com.trako.constants.TransactionConstants` to hold system-wide constants.
- `CATEGORY_TRANSFER = "TRANSFER"`

### 2. Extract Validation Logic
Create `com.trako.services.TransactionValidator` to encapsulate:
- `validateAccountOwnership`
- `validateCategoryOwnership`
- `validateTransactionOwnership`

### 3. Improve Type Safety
Create `com.trako.models.dto.TransferResult` to replace `Transaction[]`.
- Fields: `debitTransaction`, `creditTransaction`.

### 4. Extract Presentation Logic
Create `com.trako.services.TransactionPresentationService`.
- Move `hideTransferCredits` logic here.
- Move `markTransferTypeAsTransfer` logic here.
- This ensures `TransactionController` stays thin and focused on HTTP/Routing.

### 5. Refactor `TransactionWriteService`
- Inject `TransactionValidator`.
- Use `TransferResult` instead of `Transaction[]`.
- Break down `updateTransaction` into distinct private methods for clarity.
- Remove hardcoded "TRANSFER" string.

### 6. Update Consumers
- Update `TransactionController` to use `TransactionPresentationService`.
- Update `RecurringTransactionService` if it relies on changed method signatures.

## Future Steps (Out of Scope for this pass)
- Split `TransactionRequest` into `CreateTransactionRequest` and `CreateTransferRequest` to enforce clearer contracts.
