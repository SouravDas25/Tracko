import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/models/contact.dart';

class Split {
  int? id;
  int transactionId = 0;
  int userId = 0;
  int? contactId;
  double amount = 0.0;
  int isSettled = 0;
  DateTime settledAt = DateTime.now();
  Transaction? transaction;
  Contact? contact;

  Split();

  @override
  String toString() {
    return 'Split{id: $id, transactionId: $transactionId, userId: $userId, contactId: $contactId, amount: $amount, isSettled: $isSettled}';
  }
}
