import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/AccountDialog.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/screen.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/pages/transaction_list_page/transaction_list_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class AccountPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return AccountPageState();
  }
}

class AccountPageState extends AsyncLoadState<AccountPage> {
  List<Account> accounts = [];
  Map<int, double> balances = {};
  final RefreshController refreshController = RefreshController();

  @override
  void dispose() {
    refreshController.dispose();
    super.dispose();
  }

  @override
  asyncLoad() async {
    await initData();
    this.loadCompleteView();
    return null;
  }

  initData() async {
    accounts = await AccountController.getAllAccounts();
    try {
      balances = await AccountRepository().getAccountBalances();
    } catch (_) {
      balances = {};
    }
  }

  Future<void> refresh() async {
    await initData();
    if (!mounted) return;
    setState(() {
      refreshController.refreshCompleted();
    });
  }

  void deleteDialog(int id) async {
    try {
      await AccountController.deleteAccount(id);
      if (this.mounted)
        FlushDialog.flash(context, "Success", "Account Deleted.");
    } catch (e) {
      if (this.mounted) FlushDialog.flash(context, "Error", e.toString());
    }
    setState(() {
      initData();
    });
  }

  @override
  Widget completeWidget(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Accounts"),
        centerTitle: true,
      ),
      body: SmartRefresher(
        controller: refreshController,
        enablePullDown: true,
        enablePullUp: false,
        onRefresh: () async {
          await refresh();
        },
        child: ListView.builder(
          physics: const AlwaysScrollableScrollPhysics(),
          itemBuilder: (context, i) {
            return Slidable(
              // delegate: new SlidableScrollDelegate(), // Removed in flutter_slidable 3.x
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
                margin: EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                child: ListTile(
                  contentPadding:
                      EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  leading: Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: Theme.of(context).primaryColor,
                      shape: BoxShape.circle,
                      boxShadow: [
                        BoxShadow(
                          color:
                              Theme.of(context).primaryColor.withOpacity(0.3),
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
                  onTap: () {
                    final id = accounts[i].id ?? 0;
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => TransactionListPage(
                          initialAccountIds: id == 0 ? null : [id],
                        ),
                      ),
                    );
                  },
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        CommonUtil.toCurrency(
                          balances[accounts[i].id ?? 0] ?? 0.0,
                          currencyCode: accounts[i].currency,
                        ),
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                          color: Theme.of(context).textTheme.bodyLarge?.color,
                        ),
                      ),
                      const SizedBox(width: 8),
                      IconButton(
                        icon: Icon(
                          Icons.edit_outlined,
                          size: 24,
                          color: Theme.of(context).primaryColor,
                        ),
                        onPressed: () {
                          showDialog(
                            context: context,
                            builder: (_) => AccountDialog(
                              account: accounts[i],
                              callback: () {
                                setState(() {
                                  initData();
                                });
                              },
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
              endActionPane: ActionPane(
                motion: ScrollMotion(),
                children: [
                  SlidableAction(
                    onPressed: (context) {
                      deleteDialog(accounts[i].id ?? 0);
                    },
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                    icon: Icons.delete,
                    label: 'Delete',
                  ),
                ],
              ),
            );
          },
          itemCount: accounts.length,
        ),
      ),
      floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            showDialog(
              context: context,
              builder: (_) => AccountDialog(
                callback: () {
                  setState(() {
                    initData();
                  });
                },
              ),
            );
          }),
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
