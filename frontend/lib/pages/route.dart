import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/add_item_page/add_item.dart';
import 'package:tracko/pages/home_page/home_tab.dart';
import 'package:tracko/pages/login_page/login_page.dart';
import 'package:flutter/material.dart';

class Routes {
  static Map<String, WidgetBuilder> routes = {
    '/home': (BuildContext context) => new HomeTab(),
    '/login': (BuildContext context) => new LoginPage(),
    '/add_item': (BuildContext context) => new AddItemPage(),
    '/transfer': (BuildContext context) {
      final t = Transaction.defaultObject();
      t.transactionType = TransactionType.TRANSFER;
      return AddItemPage(transaction: t);
    },
  };
}
