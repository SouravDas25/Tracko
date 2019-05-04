import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';

class SetUpPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _setUpPage();
  }
}

class _setUpPage extends State<SetUpPage> {
  TextEditingController nameController = TextEditingController(),
      emailController = TextEditingController();

  final _formKey = GlobalKey<FormState>();

  updateUser() async {
    var adapter = DatabaseUtil.getAdapter();
    User user = await UserBean.getCurrentUser();
    user.name = nameController.text;
    user.email = emailController.text;
    UserBean(adapter).update(user);
    Navigator.pushReplacementNamed(
      context,
      "/home",
    );
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Screen(
      body: Form(
        key: _formKey,
        child: ListView(
          children: <Widget>[
            CircleAvatar(
              radius: 100.0,
              child: Image.asset('assets/images/user-avatar.png'),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8.0),
              child: TextFormField(
                controller: nameController,
                keyboardType: TextInputType.text,
                decoration: new InputDecoration(hintText: "Name"),
                style: TextStyle(fontSize: 20.0),
                validator: (value) {
                  if (value.isEmpty) {
                    return 'Please enter some text';
                  }
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8.0),
              child: TextFormField(
                controller: emailController,
                keyboardType: TextInputType.emailAddress,
                decoration: new InputDecoration(hintText: "Email Address"),
                style: TextStyle(fontSize: 20.0),
                validator: (value) {
                  if (value.isEmpty) {
                    return 'Please enter some text';
                  }
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(30.0),
              child: RaisedButton(
                color: Theme.of(context).primaryColor,
                textColor: Colors.white,
                onPressed: () {
                  if (_formKey.currentState.validate()) {
                    // If the form is valid, display a snackbar. In the real world, you'd
                    // often want to call a server or save the information in a database
                    updateUser();
                  }
                },
                padding: EdgeInsets.symmetric(vertical: 20.0),
                child: Text(
                  'Next',
                  style: TextStyle(fontSize: 20.0),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
