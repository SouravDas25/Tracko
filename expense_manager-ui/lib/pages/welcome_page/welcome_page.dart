import 'package:flutter/material.dart';
import 'package:expense_manager/component/menu_bar.dart';
import 'package:expense_manager/pages/phone_login_page/phone_login_page.dart';

class WelcomePage extends StatelessWidget {
  WelcomePage({Key key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: MenuBar(),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: ListView(children: [
            Image.asset("assets/images/login_img2.jpg"),
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Text(
                "Welcome to Expense Manager",
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                textAlign: TextAlign.center,
              ),
            ),
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 100.0, vertical: 20.0),
              child: RaisedButton(
                color: Theme.of(context).primaryColor,
                textColor: Colors.white,
                onPressed: () {
                  Navigator.of(context).pushNamed("/phone_login");
                },
                padding: EdgeInsets.symmetric(vertical: 20.0),
                child: Text('GO'),
              ),
            ),
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 100.0, vertical: 20.0),
              child: RaisedButton(
                color: Theme.of(context).primaryColor,
                textColor: Colors.white,
                onPressed: () {
                  Navigator.of(context).pushNamed("/home");
                },
                padding: EdgeInsets.symmetric(vertical: 20.0),
                child: Text('Home'),
              ),
            ),
          ]),
        ));
  }
}
