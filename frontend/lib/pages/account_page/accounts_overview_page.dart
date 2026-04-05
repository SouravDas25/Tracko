import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/amount_text.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/pages/transaction_list_page/transaction_list_page.dart';
import 'package:tracko/di/di.dart';

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
        balances = await sl<AccountRepository>().getAccountBalances();
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
        return Material(
          color: Colors.transparent,
          child: InkWell(
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
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: BoxDecoration(
                border: Border(
                  bottom: BorderSide(
                    color: Theme.of(context).dividerColor.withOpacity(0.08),
                    width: 0.5,
                  ),
                ),
              ),
              child: Row(
                children: [
                  Container(
                    width: 32,
                    height: 32,
                    decoration: BoxDecoration(
                      color: Theme.of(context).primaryColor,
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(
                        CommonUtil.getInitials(accounts[i].name),
                        style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          accounts[i].name,
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        SizedBox(height: 2),
                        Text(
                          accounts[i].currency,
                          style: TextStyle(
                            fontSize: 11,
                            color: Theme.of(context).hintColor,
                          ),
                        ),
                      ],
                    ),
                  ),
                  AmountText(
                    amount: balances[id] ?? 0.0,
                    currencyCode: accounts[i].currency,
                  ),
                  SizedBox(width: 4),
                  Icon(
                    Icons.chevron_right,
                    size: 20,
                    color: Theme.of(context).hintColor,
                  ),
                ],
              ),
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
