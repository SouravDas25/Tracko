import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/add_item_page/add_item.dart';
import 'package:tracko/pages/home_page/home_tab.dart';
import 'package:tracko/pages/login_page/login_page.dart';
import 'package:tracko/pages/set_up_page/set_up_page.dart';
import 'package:tracko/pages/welcome_page/welcome_page.dart';
import 'package:flutter/material.dart';

class Routes {
  static Map<String, WidgetBuilder> routes = {
    '/set_up': (BuildContext context) => new SetUpPage(),
    '/home': (BuildContext context) => new HomeTab(),
    '/login': (BuildContext context) => new LoginPage(),
    '/welcome': (BuildContext context) => new WelcomePage(),
    '/add_item': (BuildContext context) => new AddItemPage(),
    '/transfer': (BuildContext context) {
      final t = Transaction.defaultObject();
      t.transactionType = TransactionType.TRANSFER;
      t.isCountable = 0;
      return AddItemPage(transaction: t);
    },
  };
}
