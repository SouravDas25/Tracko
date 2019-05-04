import 'package:contacts_service/contacts_service.dart';
import 'package:expense_manager/component/menu_bar.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:flutter/material.dart';

//import 'package:simple_permissions/simple_permissions.dart';
import 'package:permission_handler/permission_handler.dart';

//
// ignore: camel_case_types
class select_contact extends StatefulWidget {
  createState() {
    return _select_contact();
  }
}

class CustomContact {
  final Contact contact;
  bool isChecked;

  CustomContact({
    this.contact,
    this.isChecked = false,
  });
}

class _select_contact extends State<select_contact> {
  List<Contact> _contacts = new List<Contact>();
  List<CustomContact> _uiCustomContacts = List<CustomContact>();
  List<CustomContact> _allContacts = List<CustomContact>();
  bool _isLoading = true;
  bool _isSelectedContactsView = false;
  String floatingButtonLabel;
  Color floatingButtonColor;
  IconData icon;
  int contactSelect = 0;
  bool _isButtonDisabled = true;

  TextEditingController searchController = new TextEditingController();
  String filter;

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    getContactsPermission();
    refreshContacts();
    searchController.addListener(() {
      setState(() {
        filter = searchController.text;
      });
    });
  }

  @override
  void dispose() {
    searchController.dispose();
    super.dispose();
  }

  void getContactsPermission() async {
    Map<PermissionGroup, PermissionStatus> permissions =
        await PermissionHandler()
            .requestPermissions([PermissionGroup.contacts]);
  }

  refreshContacts() async {
    setState(() {
      _isLoading = true;
    });
    var contacts = await ContactsService.getContacts();
    _populateContacts(contacts);
  }

  void _populateContacts(Iterable<Contact> contacts) {
    _contacts = contacts.where((item) => item.displayName != null).toList();
    _contacts.sort((a, b) => a.displayName.compareTo(b.displayName));
    _allContacts =
        _contacts.map((contact) => CustomContact(contact: contact)).toList();
    setState(() {
      _uiCustomContacts = _allContacts;
      _isLoading = false;
    });
  }

  void _onSubmit() {
    setState(() {
      if (!_isSelectedContactsView) {
        _uiCustomContacts =
            _allContacts.where((contact) => contact.isChecked == true).toList();
        _isSelectedContactsView = true;

        _restateFloatingButton(
          Text("Done").toString(),
          Icons.done,
          Colors.green,
        );
      } else {
        _uiCustomContacts = _allContacts;
        _isSelectedContactsView = false;
        _restateFloatingButton(
          Text("Select").toString(),
          Icons.done,
          Colors.green,
        );
      }
    });
  }

  void _restateFloatingButton(String label, IconData icon, Color color) {
    floatingButtonLabel = label;
    icon = icon;
    floatingButtonColor = color;
  }

  @override
  Widget build(BuildContext context) {
    ListTile _buildListTile(CustomContact c, List<Item> list) {
      return ListTile(
        leading: (c.contact.avatar != null)
            ? CircleAvatar(backgroundImage: MemoryImage(c.contact.avatar))
            : CircleAvatar(
                child: Text(
                    (c.contact.displayName[0] +
                        c.contact.displayName[1].toUpperCase()),
                    style: TextStyle(color: Colors.white)),
              ),
        title: Text(c.contact.displayName ?? ""),
        subtitle: list.length >= 1 && list[0]?.value != null
            ? Text(list[0].value)
            : Text(''),
        trailing: Checkbox(
            activeColor: Colors.green,
            value: c.isChecked,
            onChanged: (bool value) {
              setState(() {
                c.isChecked = value;
                if (c.isChecked == true) {
                  contactSelect++;
                } else
                  contactSelect--;
                if (contactSelect > 0)
                  _isButtonDisabled = false;
                else
                  _isButtonDisabled = true;
              });
            }),
      );
    }

    FloatingActionButton fab;
    if (!_isButtonDisabled) {
      fab = FloatingActionButton(
        onPressed: _onSubmit,
        child: Icon(Icons.arrow_forward),
      );
    }
    return Scaffold(
        appBar: AppBar(title: Text('$contactSelect')),
        body: new Column(children: <Widget>[
          new Padding(
            padding: new EdgeInsets.all(8.0),
            child: new TextField(
              controller: searchController,
              decoration: InputDecoration(
                  hintText: 'Search Contacts',
                  contentPadding: EdgeInsets.fromLTRB(20.0, 15.0, 20.0, 15.0),
                  border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(32.0))),
            ),
          ),
          new Expanded(
              child: new ListView.builder(
            itemCount: _uiCustomContacts?.length,
            itemBuilder: (BuildContext context, int index) {
              CustomContact _contact = _uiCustomContacts[index];
              var _phonesList = _contact.contact.phones.toList();
              return _buildListTile(_contact, _phonesList);
            },
          )),
        ]),
        floatingActionButton: fab);
  }
}
