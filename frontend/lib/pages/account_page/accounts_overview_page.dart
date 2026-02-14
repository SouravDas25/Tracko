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
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
          child: Card(
            elevation: 1,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16.0),
              side: BorderSide(
                  color: Theme.of(context).dividerColor.withOpacity(0.1)),
            ),
            child: ListTile(
              leading: WidgetUtil.textAvatar(accounts[i].name),
              title: Text(
                accounts[i].name,
                style: WidgetUtil.defaultTextStyle(),
              ),
              subtitle: Text(
                accounts[i].currency,
                style: TextStyle(color: Colors.grey),
              ),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    CommonUtil.toCurrency(
                      balances[id] ?? 0.0,
                      currencyCode: accounts[i].currency,
                    ),
                    style: WidgetUtil.defaultTextStyle(),
                  ),
                  const SizedBox(width: 8),
                  const Icon(Icons.chevron_right),
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
    return const Center(
      child: Text('No Data Available.'),
    );
  }
}
