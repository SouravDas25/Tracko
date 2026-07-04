import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/add_item_page/add_item.dart';
import 'package:tracko/pages/analytics_page/analytics_page.dart';
import 'package:tracko/pages/history_page/transaction_history_page.dart';
import 'package:tracko/pages/home_page/home_tab.dart';
import 'package:tracko/pages/login_page/login_page.dart';
import 'package:tracko/pages/search_page/search_page.dart';
import 'package:flutter/material.dart';

class Routes {
  static Route<dynamic>? onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case '/home':
        return MaterialPageRoute(builder: (_) => HomeTab());
      case '/login':
        return MaterialPageRoute(builder: (_) => LoginPage());
      case '/add_item':
        return MaterialPageRoute(builder: (_) => AddItemPage());
      case '/transfer':
        final t = Transaction.defaultObject();
        t.transactionType = TransactionType.TRANSFER;
        return MaterialPageRoute(builder: (_) => AddItemPage(transaction: t));
      case '/analytics':
        return MaterialPageRoute(builder: (_) => const AnalyticsPage());
      case '/search':
        return MaterialPageRoute(builder: (_) => const SearchPage());
      case '/history':
        final args = settings.arguments as Map<String, dynamic>?;
        return MaterialPageRoute(
          builder: (_) => TransactionHistoryPage(
            transactionId: args?['transactionId'] as int?,
            initialFilter: args?['initialFilter'] as String?,
          ),
        );
      default:
        return MaterialPageRoute(builder: (_) => HomeTab());
    }
  }
}
