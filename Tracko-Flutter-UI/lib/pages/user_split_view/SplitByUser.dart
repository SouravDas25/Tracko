import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/LabelWidget.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/controllers/SplitController.dart';
import 'package:tracko/models/chats.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/models/user.dart';
import 'package:flutter/cupertino.dart' hide Split;
import 'package:flutter/material.dart' hide Split;
import 'package:intl/intl.dart';

class SplitByUser extends StatefulWidget {
  final User otherUser;
  final Chat currentChat;
  final User currentUser;

  SplitByUser(this.otherUser, this.currentChat, this.currentUser);

  @override
  State<StatefulWidget> createState() {
    return _SplitList();
  }
}

class SplitExpanded {
  bool expanded;
  Split split;

  SplitExpanded(this.expanded, this.split);
}

class _SplitList extends AsyncLoadState<SplitByUser> {
  double dueAmount = 0.0;
  List<Split> splits = [];
  List<SplitExpanded> expandList = [];

  @override
  asyncLoad() async {
    await loadData();
    this.loadCompleteView();
  }

  loadData() async {
    final otherUserKey = widget.otherUser.globalId.isNotEmpty
        ? widget.otherUser.globalId
        : (widget.otherUser.id?.toString() ?? '');
    dueAmount = await SplitController.getDueAmountByUserId(otherUserKey);
    splits = await SplitController.findByUserIdKey(otherUserKey);
    expandList = splits.map((Split split) {
      return SplitExpanded(false, split);
    }).cast<SplitExpanded>().toList();
  }

  settleAllSplit(List<Split> splits, int settleTo) async {
    for (Split split in splits) {
      await SplitController.settleSplit(split, settleTo: settleTo);
    }
    if (this.mounted) {
      await loadData();
      setState(() {});
    }
  }

  menuDropDownClick(item) async {
    switch (item) {
      case "settle-all":
        {
          LoadingDialog.show(context);
          await settleAllSplit(this.splits, 1);
          await await loadData();
          await LoadingDialog.hide(context);
          this.setState(() {});
        }
        break;
      case "unsettle-all":
        {
          LoadingDialog.show(context);
          await settleAllSplit(this.splits, 0);
          await await loadData();
          await LoadingDialog.hide(context);
          this.setState(() {});
        }
        break;
    }
  }

  @override
  Widget completeWidget(BuildContext context) {
//    print(expandList[0].split.transaction.comments.runtimeType);
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        actions: <Widget>[
          PopupMenuButton<String>(
            icon: Icon(Icons.more_vert),
            itemBuilder: (context) {
              return [
                PopupMenuItem<String>(
                  child: AbsorbPointer(
                    child: TextButton.icon(
                        onPressed: () {},
                        icon: Icon(
                          Icons.check_circle,
                          color: Colors.green,
                        ),
                        label: Text("Settle All")),
                  ),
                  value: "settle-all",
                ),
                PopupMenuItem<String>(
                  child: AbsorbPointer(
                    child: TextButton.icon(
                        onPressed: () {},
                        icon: Icon(
                          Icons.clear,
                          color: Colors.red,
                        ),
                        label: Text("Unsettle All")),
                  ),
                  value: "unsettle-all",
                ),
              ];
            },
            onSelected: menuDropDownClick,
          )
        ],
        title: Text(
            " ${widget.otherUser.name} (${widget.otherUser.globalId}) ${widget.currentChat.chatGroupId}"),
      ),
//      backgroundColor: Colors.deepOrange[100],
      body: SingleChildScrollView(
        child: Column(
          children: <Widget>[
            ExpansionPanelList(
              expansionCallback: (int index, bool isExpanded) {
                setState(() {
                  expandList[index].expanded = !isExpanded;
                });
              },
              children: expandList.map<ExpansionPanel>((SplitExpanded item) {
                return ExpansionPanel(
                  headerBuilder: (BuildContext context, bool isExpanded) {
                    return ListTile(
                      onTap: () {
                        SplitController.settleSplit(item.split);
                      },
                      leading:
                          WidgetUtil.textAvatar(item.split.transaction?.name ?? ''),
                      title: Padding(
                        padding: const EdgeInsets.only(top: 12.0, bottom: 8.0),
                        child: Text(
                          item.split.transaction?.name ?? '',
                          style: TextStyle(fontSize: 20.0),
                        ),
                      ),
                      subtitle: Column(
                        mainAxisAlignment: MainAxisAlignment.start,
                        children: <Widget>[
                          if (item.split.contact != null)
                            Row(
                              children: <Widget>[
                                const Icon(Icons.person, size: 15.0, color: Colors.blueGrey),
                                const SizedBox(width: 6),
                                Expanded(
                                  child: Text(
                                    item.split.contact!.name.isNotEmpty
                                        ? item.split.contact!.name
                                        : (item.split.contact!.phoneNo),
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(fontWeight: FontWeight.w600),
                                  ),
                                ),
                              ],
                            ),
                          Row(
                            children: <Widget>[
                              Padding(
                                padding: EdgeInsets.symmetric(vertical: 2.0),
                                child: item.split.isSettled == 1
                                    ? Icon(
                                        Icons.check,
                                        size: 15.0,
                                        color: Colors.green,
                                      )
                                    : Icon(
                                        Icons.alarm,
                                        size: 15.0,
                                        color: Colors.red,
                                      ),
                              ),
                              Text(
                                item.split.isSettled == 1
                                    ? "Settled"
                                    : "Pending",
                                style: TextStyle(
                                    color: item.split.isSettled == 1
                                        ? Colors.green
                                        : Colors.red,
                                    fontWeight: FontWeight.w600),
                              )
                            ],
                          )
                        ],
                      ),
                      trailing: WidgetUtil.transformAmount2TextWidget(
                          item.split.isSettled == 1
                              ? TransactionType.CREDIT
                              : TransactionType.DEBIT,
                          item.split.amount,
                          addSign: false),
                    );
                  },
                  isExpanded: item.expanded,
                  body: Column(
                    children: <Widget>[
                      LabelWidget(
                          "Category:",
                          item.split.transaction?.category != null
                              ? item.split.transaction?.category?.name ?? ''
                              : "No Category",
                          Colors.black),
                      LabelWidget(
                          "Transaction Date:",
                          item.split.transaction?.date != null
                              ? DateFormat('dd-MMM-yyyy').format(item.split.transaction!.date)
                              : "Date Not Avaialble",
                          Colors.black),
                      LabelWidget(
                          "Total Amount:",
                          CommonUtil.toCurrency(item.split.transaction?.amount ?? 0.0),
                          Colors.orange),
                      Padding(
                        padding: EdgeInsets.symmetric(vertical: 4.0),
                        child: Text(
                          item.split.transaction?.comments != null
                              ? item.split.transaction?.comments ?? ''
                              : "",
                          textAlign: TextAlign.left,
                          style: TextStyle(
                              fontSize: 16,
                              color: Colors.black,
                              fontWeight: FontWeight.w700),
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
            new Divider(
              height: 1.0,
            ),
            new Container(
              decoration: new BoxDecoration(
                color: Theme.of(context).cardColor,
              ),
              child: ListTile(
                leading: Text(
                  "Total Amount Due ",
                  style: TextStyle(fontWeight: FontWeight.w600, fontSize: 18.0),
                ),
                trailing: dueAmount <= 0.0
                    ? Icon(
                        Icons.check,
                        size: 35.0,
                        color: Colors.green,
                      )
                    : WidgetUtil.transformAmount2TextWidget(
                        TransactionType.DEBIT, dueAmount,
                        addSign: false),
              ),
            )
          ],
        ),
      ),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Split Data Found."),
    );
  }
}
