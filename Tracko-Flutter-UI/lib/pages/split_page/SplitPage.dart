import 'package:tracko/component/interfaces.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/pages/split_page/SplitByContact.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/repositories/split_repository.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class _ContactDue {
  final Contact contact;
  final double dueAmount;

  _ContactDue(this.contact, this.dueAmount);
}

class SplitPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _SplitPage();
  }
}

class _SplitPage extends RefreshableState<SplitPage> {
  RefreshController refreshController = new RefreshController();
  final _contactRepo = ContactRepository();
  final _splitRepo = SplitRepository();
  List<_ContactDue> contacts = [];
  String? _error;

  _SplitPage();

  @override
  void didUpdateWidget(SplitPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    asyncLoad();
//    print("didUpdateWidget");
  }

  void asyncLoad() async {
    await _load();
  }

  Future<void> _load() async {
    await _refreshInternal();
    if (!mounted) return;
    if (this.contacts.isEmpty) {
      this.loadFallbackView();
    } else {
      this.loadCompleteView();
    }
  }

  @override
  void refresh() async {
    await _refreshInternal();
    if (!mounted) return;
    setState(() {
      refreshController.refreshCompleted();
    });
  }

  Future<void> _refreshInternal() async {
    try {
      _error = null;
      final list = await _contactRepo.listMine();
      final dues = await Future.wait(list.map((c) async {
        final id = c.id;
        if (id == null) return _ContactDue(c, 0.0);
        final unsettled = await _splitRepo.getUnsettledByContactId(id);
        final due = unsettled.fold(0.0, (value, s) => value + s.amount);
        return _ContactDue(c, due);
      }));
      dues.sort((a, b) => b.dueAmount.compareTo(a.dueAmount));
      this.contacts = dues;
    } catch (e) {
      _error = e.toString();
      this.contacts = [];
    }
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text("No Splits Available"),
            if (_error != null && _error!.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                textAlign: TextAlign.center,
              ),
            ],
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: () async {
                await _load();
              },
              child: const Text('Retry'),
            )
          ],
        ),
      ),
    );
  }

  @override
  Widget completeWidget(BuildContext context) {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: () {
        refresh();
      },
      child: ListView.builder(
        itemBuilder: (_, int index) {
          final row = this.contacts[index];
          return Card(
            child: ListTile(
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => SplitByContact(row.contact),
                  ),
                );
              },
              title: Text(
                row.contact.name,
                style: TextStyle(fontSize: 20.0),
              ),
              subtitle: Text(
                row.contact.phoneNo.isNotEmpty
                    ? row.contact.phoneNo
                    : row.contact.email,
              ),
              trailing: Text(
                row.dueAmount.toStringAsFixed(2),
                style: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: row.dueAmount > 0 ? Colors.red : Colors.green,
                ),
              ),
              leading: CircleAvatar(
                radius: 24.0,
                child: Text(
                  row.contact.name.isNotEmpty
                      ? row.contact.name.substring(0, 1).toUpperCase()
                      : '?',
                ),
              ),
              contentPadding: EdgeInsets.all(8.0),
            ),
          );
        },
        itemCount: this.contacts.length,
      ),
    );
  }
}
