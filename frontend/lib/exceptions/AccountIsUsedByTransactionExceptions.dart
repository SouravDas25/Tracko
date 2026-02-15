class AccountIsUsedByTransactionExceptions implements Exception {
  AccountIsUsedByTransactionExceptions();

  String toString() {
    return "Account is used by transaction(s).";
  }
}
