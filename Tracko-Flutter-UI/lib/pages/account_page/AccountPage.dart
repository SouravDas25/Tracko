import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AccountDialog.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/screen.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/models/account.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class AccountPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return AccountPageState();
  }
}

class AccountPageState extends AsyncLoadState<AccountPage> {
  List<Account> accounts = [];

  @override
  asyncLoad() async {
    await initData();
    this.loadCompleteView();
    return null;
  }

  initData() async {
    accounts = await AccountController.getAllAccounts();
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
      body: ListView.builder(
        itemBuilder: (context, i) {
          return Slidable(
            // delegate: new SlidableScrollDelegate(), // Removed in flutter_slidable 3.x
            child: Card(
              margin: EdgeInsets.all(2),
              child: ListTile(
                contentPadding: EdgeInsets.all(8),
                leading: WidgetUtil.textAvatar(accounts[i].name),
                title: Text(
                  accounts[i].name,
                  style: WidgetUtil.defaultTextStyle(),
                ),
                onTap: () {
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
                trailing: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Icon(
                    Icons.edit,
                    size: 30,
                    color: Colors.blueAccent,
                  ),
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
