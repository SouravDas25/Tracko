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
import 'package:tracko/di/di.dart';

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

  late final SplitRepository _splitRepo;
  late final TransactionRepository _txRepo;

  @override
  void initState() {
    _splitRepo = sl<SplitRepository>();
    _txRepo = sl<TransactionRepository>();
    super.initState();
  }

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
              padding:
                  const EdgeInsets.symmetric(horizontal: 12.0, vertical: 10.0),
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
                return Container(
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
                  margin: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  child: ListTile(
                    contentPadding:
                        EdgeInsets.symmetric(horizontal: 16, vertical: 8),
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
                    leading: Container(
                      width: 40,
                      height: 40,
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
                          CommonUtil.getInitials(item.transaction?.name ?? ''),
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                        ),
                      ),
                    ),
                    title: Text(
                      item.transaction?.name ?? '',
                      style: TextStyle(
                        fontSize: 16.0,
                        fontWeight: FontWeight.w600,
                        color: Theme.of(context).textTheme.bodyLarge?.color,
                      ),
                    ),
                    subtitle: Row(
                      children: <Widget>[
                        Padding(
                          padding: EdgeInsets.symmetric(vertical: 2.0),
                          child: item.isSettled == 1
                              ? Icon(
                                  Icons.check_circle,
                                  size: 14.0,
                                  color: Colors.green,
                                )
                              : Icon(
                                  Icons.schedule,
                                  size: 14.0,
                                  color: Colors.red,
                                ),
                        ),
                        SizedBox(width: 4),
                        Text(
                          item.isSettled == 1 ? "Settled" : "Pending",
                          style: TextStyle(
                              fontSize: 13,
                              color: item.isSettled == 1
                                  ? Colors.green
                                  : Colors.red,
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
                      ],
                    ),
                  ),
                );
              },
            ),
            Container(
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
              margin: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: ListTile(
                contentPadding:
                    EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                leading: Text(
                  "Total Amount Due ",
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 18.0,
                    color: Theme.of(context).textTheme.bodyLarge?.color,
                  ),
                ),
                trailing: dueAmount <= 0.0
                    ? Container(
                        padding: EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.green.withOpacity(0.1),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.check,
                          size: 24.0,
                          color: Colors.green,
                        ),
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
              Icons.receipt_long_rounded,
              size: 48,
              color: Theme.of(context).disabledColor.withOpacity(0.5),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            "No Split Data Found",
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).hintColor,
            ),
          ),
        ],
      ),
    );
  }
}
