import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:flutter/material.dart';
import 'package:tracko/di/di.dart';

class SelectBackendContactPage extends StatefulWidget {
  createState() {
    return _SelectBackendContactList();
  }
}

class _CustomContact {
  final Contact contact;
  bool isChecked;

  _CustomContact({required this.contact, this.isChecked = false});
}

class _SelectBackendContactList
    extends AsyncLoadState<SelectBackendContactPage> {
  late final ContactRepository _repo;

  List<_CustomContact> visibleContacts = <_CustomContact>[];
  List<_CustomContact> allContacts = <_CustomContact>[];
  List<_CustomContact> selectedContacts = <_CustomContact>[];

  bool _isButtonDisabled = true;
  int contactSelect = 0;

  TextEditingController searchController = new TextEditingController();
  String filter = '';

  @override
  void initState() {
    _repo = sl<ContactRepository>();
    super.initState();
  }

  @override
  void dispose() {
    searchController.dispose();
    super.dispose();
  }

  void searchContact() {
    setState(() {
      filter = searchController.text.toLowerCase();
      visibleContacts = allContacts
          .where((customContact) =>
              customContact.contact.name.toLowerCase().contains(filter))
          .toList();
    });
  }

  @override
  asyncLoad() async {
    await refreshContacts();
    searchController.addListener(searchContact);
    this.loadCompleteView();
  }

  Future<void> refreshContacts() async {
    final list = await _repo.listMine();
    allContacts = list
        .where((c) => c.id != null)
        .map((c) => _CustomContact(contact: c))
        .toList();
    allContacts.sort((a, b) => a.contact.name.compareTo(b.contact.name));
    visibleContacts = allContacts;
  }

  void _onSubmit() async {
    final returningContact =
        selectedContacts.map((c) => c.contact).toList(growable: false);
    Navigator.pop(context, returningContact);
    setState(() {});
  }

  void onCheckBoxHit(_CustomContact customContact, bool value) {
    setState(() {
      customContact.isChecked = value;
      if (customContact.isChecked == true) {
        contactSelect++;
      } else {
        contactSelect--;
      }

      _isButtonDisabled = contactSelect <= 0;
      if (value) {
        selectedContacts.add(customContact);
      } else {
        selectedContacts.remove(customContact);
      }
    });
  }

  @override
  Widget completeWidget(BuildContext context) {
    ListTile _buildListTile(_CustomContact customContact) {
      return ListTile(
        leading: WidgetUtil.textAvatar(customContact.contact.name),
        title: Text(customContact.contact.name),
        subtitle: Text(
          customContact.contact.phoneNo.isNotEmpty
              ? customContact.contact.phoneNo
              : customContact.contact.email,
        ),
        trailing: Checkbox(
            activeColor: Colors.green,
            value: customContact.isChecked,
            onChanged: (bool? value) =>
                onCheckBoxHit(customContact, value ?? false)),
      );
    }

    AppBar appBar = AppBar(
      title: Text('Select Contacts'),
    );
    if (!_isButtonDisabled) {
      appBar = AppBar(
        title: Text('Select Contacts'),
        actions: <Widget>[
          TextButton(
            child: Icon(
              Icons.check,
              color: Colors.greenAccent,
              size: 30.0,
            ),
            onPressed: _onSubmit,
          )
        ],
      );
    }

    List<Widget> body = [];
    body.add(Padding(
      padding: EdgeInsets.only(top: 8.0, right: 8.0, left: 8.0, bottom: 2.0),
      child: TextField(
        controller: searchController,
        decoration: InputDecoration(
            hintText: 'Search Contacts',
            contentPadding: EdgeInsets.fromLTRB(20.0, 15.0, 20.0, 15.0),
            border:
                OutlineInputBorder(borderRadius: BorderRadius.circular(10.0))),
      ),
    ));

    if (selectedContacts.isNotEmpty) {
      body.add(SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
            children: selectedContacts
                .map((cc) => Chip(
                      avatar: WidgetUtil.textAvatar(cc.contact.name),
                      onDeleted: () => onCheckBoxHit(cc, false),
                      label: Text(cc.contact.name),
                    ))
                .toList()),
      ));
    }

    body.add(Expanded(
        child: ListView.builder(
      itemCount: visibleContacts.length,
      itemBuilder: (BuildContext context, int index) {
        _CustomContact _contact = visibleContacts[index];
        return _buildListTile(_contact);
      },
    )));

    return Scaffold(
      appBar: appBar,
      body: Column(
        children: body,
      ),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Text("No Contacts Found");
  }
}
