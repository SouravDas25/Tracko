import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';
import 'package:expense_manager/component/menu_bar.dart';
import 'package:expense_manager/pages/phone_login_page/phone_login_page.dart';

class WelcomePage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return new _WelcomePage();
  }
}

class _WelcomePage extends State<WelcomePage> {
  User user;
  bool loading = true;

  @override
  initState() {
    super.initState();
    this.loadData();
  }

  loadData() async {
    if (this.user == null) {
      this.user = await UserBean.getCurrentUser();
      await Future<void>.delayed(new Duration(seconds: 3));
      setState(() {
        if (user != null) {
          Navigator.pushReplacementNamed(
            context,
            "/home",
          );
        }
      });
      print(this.user);
    }
    loading = false;
  }

  @override
  Widget build(BuildContext context) {
    if (this.user != null) {
      return new Container();
    }
    Widget loaderOrButton;
    if (loading) {
      loaderOrButton = new Center(
        child: new CircularProgressIndicator(),
      );
    } else {
      loaderOrButton = new RaisedButton(
        color: Theme.of(context).primaryColor,
        textColor: Colors.white,
        onPressed: () {
          Navigator.pushReplacementNamed(
            context,
            "/phone_login",
          );
        },
        padding: EdgeInsets.symmetric(vertical: 20.0),
        child: Text('Login',style: TextStyle(
          fontSize: 18.0
        ),),
      );
    }
    return Scaffold(
        appBar: MenuBar(),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: ListView(children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal : 30.0,vertical: 75.0),
              child: Center(child: Image.asset("assets/images/expense-icon.png")),
            ),
            Padding(
              padding: const EdgeInsets.all(0.0),
              child: Text(
                "Welcome to",
                style: TextStyle(fontSize: 16.0, fontWeight: FontWeight.w600),
                textAlign: TextAlign.center,
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(
                "Tracko",
                style: TextStyle(fontSize: 30.0, fontWeight: FontWeight.w600),
                textAlign: TextAlign.center,
              ),
            ),
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 0.0, vertical: 20.0),
              child: loaderOrButton,
            ),
          ]),
        ));
  }
}
