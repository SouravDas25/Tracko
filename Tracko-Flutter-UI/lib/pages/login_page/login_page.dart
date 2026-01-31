import 'package:tracko/component/menu_bar.dart' as TrackoMenuBar;
import 'package:flutter/material.dart';

class LoginPage extends StatelessWidget {
  LoginPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: TrackoMenuBar.MenuBar(),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: ListView(children: [
            Image.asset("assets/login_img2.jpg"),
            LoginForm(),
          ]),
        ));
  }
}

class LoginForm extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _LoginPage();
  }
}

class _LoginPage extends State<LoginForm> {
  final _formKey = GlobalKey<FormState>();

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        children: <Widget>[
          TextFormField(
            decoration: new InputDecoration(hintText: "E-Mail"),
            validator: (value) {
              if (value?.isEmpty ?? true) {
                return 'Please enter some text';
              }
            },
          ),
          TextFormField(
            decoration: new InputDecoration(hintText: "Password"),
            validator: (value) {
              if (value?.isEmpty ?? true) {
                return 'Please enter some text';
              }
            },
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16.0),
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                foregroundColor: Colors.white,
              ),
              onPressed: () {
                // Validate will return true if the form is valid, or false if
                // the form is invalid.
                if (_formKey.currentState?.validate() ?? false) {
                  // If the form is valid, we want to show a Snackbar
                  ScaffoldMessenger.of(context)
                      .showSnackBar(SnackBar(content: Text('Processing Data')));
                }
              },
              child: Text('Login'),
            ),
          ),
        ],
      ),
    );
  }
}
