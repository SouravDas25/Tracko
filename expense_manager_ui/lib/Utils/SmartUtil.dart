


import 'package:expense_manager/models/PossibleTransaction.dart';

class SmartUtil {

  static List<PossibleTransaction> possibleTransactions = List<PossibleTransaction>();

  static List<PossibleTransaction> getPT() {
    return possibleTransactions;
  }

}