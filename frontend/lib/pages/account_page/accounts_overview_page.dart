import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/pages/transaction_list_page/transaction_list_page.dart';

class AccountsOverviewPage extends StatefulWidget {
  const AccountsOverviewPage({super.key});

  @override
  State<StatefulWidget> createState() => _AccountsOverviewPageState();
}

class _AccountsOverviewPageState extends AsyncLoadState<AccountsOverviewPage> {
  List<Account> accounts = [];
  Map<int, double> balances = {};

  @override
  asyncLoad() async {
    try {
      accounts = await AccountController.getAllAccounts();
      try {
        balances = await AccountRepository().getAccountBalances();
      } catch (_) {
        balances = {};
      }
      loadCompleteView();
    } catch (e) {
      loadFallbackView();
    }
  }

  @override
  Widget completeWidget(BuildContext context) {
    return ListView.builder(
      itemCount: accounts.length,
      itemBuilder: (context, i) {
        final id = accounts[i].id ?? 0;
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 6.0),
          child: Container(
            decoration: BoxDecoration(
              color: Theme.of(context).cardColor,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.04),
                  blurRadius: 10,
                  offset: Offset(0, 4),
                ),
              ],
              border: Border.all(
                color: Theme.of(context).dividerColor.withOpacity(0.05),
              ),
            ),
            child: ListTile(
              contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              leading: Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: Theme.of(context).primaryColor,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: Theme.of(context).primaryColor.withOpacity(0.3),
                      blurRadius: 8,
                      offset: Offset(0, 2),
                    ),
                  ],
                ),
                child: Center(
                  child: Text(
                    CommonUtil.getInitials(accounts[i].name),
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                ),
              ),
              title: Text(
                accounts[i].name,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Theme.of(context).textTheme.bodyLarge?.color,
                ),
              ),
              subtitle: Padding(
                padding: const EdgeInsets.only(top: 4.0),
                child: Text(
                  accounts[i].currency,
                  style: TextStyle(
                    fontSize: 13,
                    color: Theme.of(context).hintColor,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    CommonUtil.toCurrency(
                      balances[id] ?? 0.0,
                      currencyCode: accounts[i].currency,
                    ),
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                      color: Theme.of(context).textTheme.bodyLarge?.color,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    Icons.chevron_right,
                    color: Theme.of(context).primaryColor,
                  ),
                ],
              ),
              onTap: () {
                if (id == 0) return;
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => TransactionListPage(
                      initialAccountIds: [id],
                    ),
                  ),
                );
              },
            ),
          ),
        );
      },
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Theme.of(context).disabledColor.withOpacity(0.05),
              shape: BoxShape.circle,
            ),
            child: Icon(
              Icons.account_balance_wallet_rounded,
              size: 48,
              color: Theme.of(context).disabledColor.withOpacity(0.5),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            "No accounts found",
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).hintColor,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            "Tap + to create a new account",
            style: TextStyle(
              fontSize: 14,
              color: Theme.of(context).disabledColor,
            ),
          ),
        ],
      ),
    );
  }
}
