

class TransactionType {
  static const int DEBIT = 1;
  static const int CREDIT = 2;

  static String stringify(int type) {
    switch (type) {
      case DEBIT:
        return "DEBIT";
      case CREDIT:
        return "CREDIT";
      default:
        return "UNKNOWN";
    }
  }

  static int inttify(String val) {
    if (val.trim().toUpperCase() == "DEBIT") {
      return TransactionType.DEBIT;
    }
    return TransactionType.CREDIT;
  }
}
