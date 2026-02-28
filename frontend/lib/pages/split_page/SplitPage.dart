import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/pages/split_page/SplitByContact.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/repositories/split_repository.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:tracko/di/di.dart';

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
  late final ContactRepository _contactRepo;
  late final SplitRepository _splitRepo;
  List<_ContactDue> contacts = [];
  String? _error;

  _SplitPage();

  @override
  void initState() {
    super.initState();
    _contactRepo = sl<ContactRepository>();
    _splitRepo = sl<SplitRepository>();
  }

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
        padding: const EdgeInsets.all(32.0),
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
                Icons.call_split_rounded,
                size: 48,
                color: Theme.of(context).disabledColor.withOpacity(0.5),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              "No Splits Available",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
              ),
            ),
            if (_error != null && _error!.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.red),
              ),
            ],
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () async {
                await _load();
              },
              style: ElevatedButton.styleFrom(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                ),
                padding: EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              ),
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
            margin: EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            child: ListTile(
              contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => SplitByContact(row.contact),
                  ),
                );
              },
              title: Text(
                row.contact.name,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Theme.of(context).textTheme.bodyLarge?.color,
                ),
              ),
              subtitle: Padding(
                padding: const EdgeInsets.only(top: 4.0),
                child: Text(
                  row.contact.phoneNo.isNotEmpty
                      ? row.contact.phoneNo
                      : row.contact.email,
                  style: TextStyle(
                    fontSize: 13,
                    color: Theme.of(context).hintColor,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              trailing: Text(
                CommonUtil.toCurrency(row.dueAmount),
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                  color: row.dueAmount > 0
                      ? Colors.red.shade400
                      : Colors.green.shade600,
                ),
              ),
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
                    row.contact.name.isNotEmpty
                        ? row.contact.name.substring(0, 1).toUpperCase()
                        : '?',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                ),
              ),
            ),
          );
        },
        itemCount: this.contacts.length,
      ),
    );
  }
}
