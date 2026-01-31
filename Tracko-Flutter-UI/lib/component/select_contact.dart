import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/services/SessionService.dart';
// import 'package:contacts_service/contacts_service.dart'; // TODO: Replace with AGP 8+ compatible alternative
import 'package:flutter/material.dart';


//
// ignore: camel_case_types
class SelectContactPage extends StatefulWidget {
  createState() {
    return SelectContactList();
  }
}

class CustomContact {
  final TrakoContact? contact;
  bool isChecked;

  CustomContact({
    this.contact,
    this.isChecked = false,
  });
}

class SelectContactList extends AsyncLoadState<SelectContactPage> {
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

  initContactsData() async {
    bool b = await CommonUtil.getContactsPermission();
    if (b) {
      await refreshContacts();
      searchController.addListener(searchContact);
    } else {
      FlushDialog.flash(context, "Permission Denied",
          "You have to grant contacts permission to use this feature.");
    }
  }

  void searchContact() {
    setState(() {
      filter = searchController.text.toLowerCase();
      visibleContacts = allContacts
          .where((customContact) =>
      customContact.contact?.name?.toLowerCase().contains(filter) == true)
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
    void initContacts() async {
      // TODO: Re-enable after replacing contacts_service with AGP 8+ compatible alternative
      // var contacts = await ContactsService.getContacts(withThumbnails: false);
      // _populateContacts(contacts);
    }

    void _populateContacts(Iterable<dynamic> contacts) {
      allContacts.clear();
      for (var contact in contacts) {
        if (contact.displayName != null) {
          allContacts
              .add(CustomContact(contact: TrakoContact.fromContact(contact)));
        }
      }
      allContacts.sort((a, b) => (a.contact?.name ?? '').compareTo(b.contact?.name ?? ''));
      setState(() {
        visibleContacts = allContacts;
      });
    }
    setState(() {
      visibleContacts = allContacts;
    });
  }

  void _onSubmit() async {
    List<TrakoContact> returningContact = [];
    for (CustomContact c in selectedContacts) {
      if (c.contact != null) returningContact.add(c.contact!);
    }
    TrakoContact rootUserContact = SessionService.currentUserContact();
    returningContact.add(rootUserContact);
    Navigator.pop(context, returningContact);
    setState(() {});
  }

  void onCheckBoxHit(CustomContact customContact, bool value) {
    if (customContact.contact?.phoneNo == null ||
        (customContact.contact?.phoneNo?.length ?? 0) <= 0) {
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
        leading: WidgetUtil.textAvatar(customContact.contact?.name ?? ''),
        title: Text(customContact.contact?.name ?? ""),
        subtitle: customContact.contact?.phoneNo != null
            ? Text(customContact.contact?.phoneNo ?? '')
            : Text(''),
        trailing: Checkbox(
            activeColor: Colors.green,
            value: customContact.isChecked,
            onChanged: (bool? value) => onCheckBoxHit(customContact, value ?? false)),
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
                .map((CustomContact cc) =>
                Chip(
                  avatar: WidgetUtil.textAvatar(cc.contact?.name ?? ''),
                  onDeleted: () => onCheckBoxHit(cc, false),
                  label: Text(cc.contact?.name ?? ''),
                ))
                .toList()),
      ));
    }
    body.add(new Expanded(
        child: new ListView.builder(
          itemCount: visibleContacts?.length,
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
