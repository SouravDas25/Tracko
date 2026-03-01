import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:flutter/material.dart';
import 'package:tracko/di/di.dart';

//
// ignore: camel_case_types
class SelectContactPage extends StatefulWidget {
  createState() {
    return SelectContactList();
  }
}

class CustomContact {
  final Contact contact;
  bool isChecked;

  CustomContact({
    required this.contact,
    this.isChecked = false,
  });
}

class SelectContactList extends AsyncLoadState<SelectContactPage> {
  late final ContactRepository _repo;
  List<CustomContact> visibleContacts = <CustomContact>[];
  List<CustomContact> allContacts = <CustomContact>[];
  List<CustomContact> selectedContacts = <CustomContact>[];
  String floatingButtonLabel = '';
  late Color floatingButtonColor;
  late IconData icon;
  int contactSelect = 0;
  bool _isButtonDisabled = true;

  TextEditingController searchController = new TextEditingController();
  String filter = '';

  @override
  void initState() {
    _repo = sl<ContactRepository>();
    super.initState();
  }

  initContactsData() async {
    await refreshContacts();
    searchController.addListener(searchContact);
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
  void dispose() {
    searchController.dispose();
    super.dispose();
  }

  refreshContacts() async {
    setState(() {});
    final list = await _repo.listMine();
    allContacts = list
        .where((c) => c.id != null)
        .map((c) => CustomContact(contact: c))
        .toList();
    allContacts.sort((a, b) => a.contact.name.compareTo(b.contact.name));
    setState(() {
      visibleContacts = allContacts;
    });
  }

  void _onSubmit() async {
    final returningContact =
        selectedContacts.map((c) => c.contact).toList(growable: false);
    Navigator.pop(context, returningContact);
    setState(() {});
  }

  void onCheckBoxHit(CustomContact customContact, bool value) {
    if (customContact.contact.phoneNo.isEmpty) {
      WidgetUtil.toast("No contact number associated.");
      return;
    }
    setState(() {
      customContact.isChecked = value;
      if (customContact.isChecked == true) {
        contactSelect++;
      } else
        contactSelect--;

      if (contactSelect > 0)
        _isButtonDisabled = false;
      else
        _isButtonDisabled = true;
      if (value)
        selectedContacts.add(customContact);
      else
        selectedContacts.remove(customContact);
    });
  }

  @override
  asyncLoad() async {
    await initContactsData();
    this.loadCompleteView();
  }

  @override
  Widget completeWidget(BuildContext context) {
    ListTile _buildListTile(CustomContact customContact) {
      return ListTile(
        leading: WidgetUtil.textAvatar(customContact.contact.name),
        title: Text(customContact.contact.name),
        subtitle: Text(customContact.contact.phoneNo),
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
    body.add(new Padding(
      padding:
          new EdgeInsets.only(top: 8.0, right: 8.0, left: 8.0, bottom: 2.0),
      child: new TextField(
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
                .map((CustomContact cc) => Chip(
                      avatar: WidgetUtil.textAvatar(cc.contact.name),
                      onDeleted: () => onCheckBoxHit(cc, false),
                      label: Text(cc.contact.name),
                    ))
                .toList()),
      ));
    }
    body.add(new Expanded(
        child: new ListView.builder(
      itemCount: visibleContacts.length,
      itemBuilder: (BuildContext context, int index) {
        CustomContact _contact = visibleContacts[index];
        return _buildListTile(_contact);
      },
    )));
    return Scaffold(
        appBar: appBar,
        body: Column(
          children: body,
        ));
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Text("No Contacts Found");
  }
}
