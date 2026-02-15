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
              child: Card(
                elevation: 1,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16.0),
                  side: BorderSide(
                      color: Theme.of(context).dividerColor.withOpacity(0.1)),
                ),
                margin: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                child: ListTile(
                  contentPadding: EdgeInsets.all(8),
                  leading: WidgetUtil.textAvatar(accounts[i].name),
                  title: Text(
                    accounts[i].name,
                    style: WidgetUtil.defaultTextStyle(),
                  ),
                  subtitle: Text(
                    accounts[i].currency,
                    style: TextStyle(color: Colors.grey),
                  ),
                  onTap: () {
                    final id = accounts[i].id ?? 0;
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => TransactionListPage(
                          initialAccountIds: id == 0 ? null : [id],
                          showAccountFilter: false,
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
                        style: WidgetUtil.defaultTextStyle(),
                      ),
                      const SizedBox(width: 8),
                      IconButton(
                        icon: const Icon(
                          Icons.edit,
                          size: 30,
                          color: Colors.blueAccent,
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
    return Screen(
      titleName: "Accounts",
      body: Center(
        child: Text("No accounts found."),
      ),
    );
  }
}
