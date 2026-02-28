import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/ContactDialog.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/screen.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/di/di.dart';

class ContactPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _ContactPageState();
  }
}

class _ContactPageState extends AsyncLoadState<ContactPage> {
  late final ContactRepository _repo;
  List<Contact> contacts = [];

  @override
  void initState() {
    super.initState();
    _repo = sl<ContactRepository>();
  }

  @override
  asyncLoad() async {
    await initData();
    this.loadCompleteView();
    return null;
  }

  Future<void> initData() async {
    contacts = await _repo.listMine();
  }

  void deleteDialog(int id) async {
    try {
      await _repo.delete(id);
      if (this.mounted) {
        FlushDialog.flash(context, "Success", "Contact Deleted.");
      }
    } catch (e) {
      if (this.mounted) {
        FlushDialog.flash(context, "Error", e.toString());
      }
    }
    setState(() {
      initData();
    });
  }

  @override
  Widget completeWidget(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Contacts"),
        centerTitle: true,
      ),
      body: ListView.builder(
        itemBuilder: (context, i) {
          final c = contacts[i];
          return Slidable(
            child: Card(
              margin: EdgeInsets.all(2),
              child: ListTile(
                contentPadding: EdgeInsets.all(8),
                leading: WidgetUtil.textAvatar(c.name),
                title: Text(
                  c.name,
                  style: WidgetUtil.defaultTextStyle(),
                ),
                subtitle: Text(
                  c.phoneNo.isNotEmpty ? c.phoneNo : c.email,
                ),
                onTap: () {
                  showDialog(
                    context: context,
                    builder: (_) => ContactDialog(
                      contact: c,
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
                    deleteDialog(c.id ?? 0);
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
        itemCount: contacts.length,
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
          showDialog(
            context: context,
            builder: (_) => ContactDialog(
              callback: () {
                setState(() {
                  initData();
                });
              },
            ),
          );
        },
      ),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Screen(
      titleName: "Contacts",
      body: Center(
        child: Text("No contacts found."),
      ),
    );
  }
}
