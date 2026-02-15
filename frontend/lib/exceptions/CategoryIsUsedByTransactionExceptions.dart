class CategoryIsUsedByTransactionExceptions implements Exception {
  CategoryIsUsedByTransactionExceptions();

  String toString() {
    return "Category is used by transaction.";
  }
}
