class TransactionType {
  static const int DEBIT = 1;
  static const int CREDIT = 2;
  static const int TRANSFER = 3;

  static String stringify(int type) {
    switch (type) {
      case DEBIT:
        return "DEBIT";
      case CREDIT:
        return "CREDIT";
      case TRANSFER:
        return "TRANSFER";
      default:
        return "UNKNOWN";
    }
  }

  static int inttify(String val) {
    final v = val.trim().toUpperCase();
    if (v == "DEBIT") {
      return TransactionType.DEBIT;
    }
    if (v == "TRANSFER") {
      return TransactionType.TRANSFER;
    }
    return TransactionType.CREDIT;
  }
}
