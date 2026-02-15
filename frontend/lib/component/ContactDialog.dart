import 'package:flutter/material.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/repositories/contact_repository.dart';

class ContactDialog extends StatelessWidget {
  final Function callback;
  Contact? contact;
  bool isEdit = false;

  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();

  ContactDialog({required this.callback, this.contact}) {
    if (this.contact != null) {
      isEdit = true;
      _nameController.text = this.contact?.name ?? '';
      _phoneController.text = this.contact?.phoneNo ?? '';
      _emailController.text = this.contact?.email ?? '';
    } else {
      this.contact = Contact();
    }
  }

  Future<void> upsertContact() async {
    final name = _nameController.text.trim();
    if (name.length <= 0) {
      return;
    }

    final c = contact ?? Contact();
    c.name = name;
    c.phoneNo = _phoneController.text.trim();
    c.email = _emailController.text.trim();

    final repo = ContactRepository();
    if (c.id == null) {
      final created = await repo.create(c);
      c.id = created.id;
    } else {
      await repo.update(c.id!, c);
    }

    callback();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(isEdit ? "Update Contact" : "Add Contact"),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _nameController,
            decoration: InputDecoration(hintText: 'Name'),
          ),
          SizedBox(height: 10),
          TextField(
            controller: _phoneController,
            decoration: InputDecoration(hintText: 'Phone'),
          ),
          SizedBox(height: 10),
          TextField(
            controller: _emailController,
            decoration: InputDecoration(hintText: 'Email'),
          ),
        ],
      ),
      actions: <Widget>[
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
          },
          child: Text("Cancel"),
        ),
        ElevatedButton(
          onPressed: () async {
            await upsertContact();
            Navigator.pop(context);
          },
          child: Text(isEdit ? "Update" : "Add"),
        )
      ],
    );
  }
}
