import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/LabelWidget.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/controllers/CategoryController.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/repositories/split_repository.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:flutter/material.dart' hide Split;
import 'package:intl/intl.dart';

class SplitByContact extends StatefulWidget {
  final Contact contact;

  SplitByContact(this.contact);

  @override
  State<StatefulWidget> createState() {
    return _SplitByContactState();
  }
}

class _SplitByContactState extends AsyncLoadState<SplitByContact> {
  double dueAmount = 0.0;
  List<Split> splits = [];

  final _splitRepo = SplitRepository();
  final _txRepo = TransactionRepository();

  @override
  asyncLoad() async {
    await loadData();
    this.loadCompleteView();
  }

  loadData() async {
    final id = widget.contact.id;
    if (id == null) {
      dueAmount = 0.0;
      splits = <Split>[];
      return;
    }

    final unsettled = await _splitRepo.getUnsettledByContactId(id);
    dueAmount = unsettled.fold(0.0, (value, s) => value + s.amount);

    splits = await _splitRepo.getByContactId(id);

    for (final split in splits) {
      split.contactId = id;
      split.contact = widget.contact;
      if (split.transactionId == 0) continue;
      try {
        split.transaction = await _txRepo.getById(split.transactionId);
        if (split.transaction != null) {
          split.transaction!.category =
              await CategoryController.findById(split.transaction!.categoryId);
        }
      } catch (e) {
        // ignore
      }
    }

  }

  settleAllSplit(List<Split> splits, int settleTo) async {
    for (Split split in splits) {
      if (split.id == null) continue;
      try {
        if (settleTo == 1) {
          await _splitRepo.settle(split.id!);
        } else {
          await _splitRepo.unsettle(split.id!);
        }
      } catch (e) {
        split.isSettled = settleTo;
        if (split.isSettled == 1) split.settledAt = DateTime.now();
        await _splitRepo.update(split.id!, split);
      }
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
          await loadData();
          await LoadingDialog.hide(context);
          this.setState(() {});
        }
        break;
      case "unsettle-all":
        {
          LoadingDialog.show(context);
          await settleAllSplit(this.splits, 0);
          await loadData();
          await LoadingDialog.hide(context);
          this.setState(() {});
        }
        break;
    }
  }

  @override
  Widget completeWidget(BuildContext context) {
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
        title: Text(widget.contact.name),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 10.0),
              child: Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green,
                        foregroundColor: Colors.white,
                      ),
                      onPressed: () async {
                        LoadingDialog.show(context);
                        await settleAllSplit(this.splits, 1);
                        await loadData();
                        await LoadingDialog.hide(context);
                        setState(() {});
                      },
                      child: const Text('Settle'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.red,
                        foregroundColor: Colors.white,
                      ),
                      onPressed: () async {
                        LoadingDialog.show(context);
                        await settleAllSplit(this.splits, 0);
                        await loadData();
                        await LoadingDialog.hide(context);
                        setState(() {});
                      },
                      child: const Text('Unsettle'),
                    ),
                  ),
                ],
              ),
            ),
            ListView.builder(
              physics: NeverScrollableScrollPhysics(),
              shrinkWrap: true,
              itemCount: splits.length,
              itemBuilder: (context, index) {
                final item = splits[index];
                return ListTile(
                  dense: true,
                  visualDensity: VisualDensity.compact,
                  onTap: () async {
                    if (item.id == null) return;
                    try {
                      if (item.isSettled == 1) {
                        await _splitRepo.unsettle(item.id!);
                      } else {
                        await _splitRepo.settle(item.id!);
                      }
                    } catch (e) {
                      // ignore
                    }
                    await loadData();
                    if (mounted) setState(() {});
                  },
                  leading: WidgetUtil.textAvatar(item.transaction?.name ?? ''),
                  title: Text(
                    item.transaction?.name ?? '',
                    style: TextStyle(fontSize: 18.0),
                  ),
                  subtitle: Row(
                    children: <Widget>[
                      Padding(
                        padding: EdgeInsets.symmetric(vertical: 2.0),
                        child: item.isSettled == 1
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
                        item.isSettled == 1 ? "Settled" : "Pending",
                        style: TextStyle(
                            color: item.isSettled == 1 ? Colors.green : Colors.red,
                            fontWeight: FontWeight.w600),
                      )
                    ],
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      WidgetUtil.transformAmount2TextWidget(
                          item.isSettled == 1
                              ? TransactionType.CREDIT
                              : TransactionType.DEBIT,
                          item.amount,
                          addSign: false),
                      const SizedBox(width: 8),
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                        tooltip: 'Settle',
                        icon: const Icon(Icons.check_circle, size: 18),
                        color: Colors.green,
                        onPressed: item.isSettled == 1
                            ? null
                            : () async {
                                if (item.id == null) return;
                                await _splitRepo.settle(item.id!);
                                await loadData();
                                if (mounted) setState(() {});
                              },
                      ),
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                        tooltip: 'Unsettle',
                        icon: const Icon(Icons.undo, size: 18),
                        color: Colors.red,
                        onPressed: item.isSettled == 0
                            ? null
                            : () async {
                                if (item.id == null) return;
                                await _splitRepo.unsettle(item.id!);
                                await loadData();
                                if (mounted) setState(() {});
                              },
                      ),
                    ],
                  ),
                );
              },
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
