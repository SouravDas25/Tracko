import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/menu_bar.dart' as custom;
import 'package:tracko/models/user.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';

class WelcomePage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return new _WelcomePage();
  }
}

class _WelcomePage extends State<WelcomePage> {
  User? user;
  bool loading = true;
  bool isUserValid = false;

  @override
  initState() {
    super.initState();
    this.loadData();
    // this.getPermissions();
  }

  void getPermissions() async {
    bool b = await CommonUtil.getContactsPermission();
    if (!b && this.mounted) {
      FlushDialog.flash(context, "Permision Required",
          "Contact permission is required for this app.");
    }
    b = await CommonUtil.getSmsPermission();
    if (!b && this.mounted) {
      FlushDialog.flash(context, "Permision Required",
          "Sms permission is required for this app.");
    }
  }

  loadData() async {
    if (this.user == null) {
      final u = await SessionService.getCurrentUser();
      this.user = u;
      this.isUserValid = u != null ? await SessionService.loginUser(u) : false;
      print(this.user);

      if (this.isUserValid && mounted) {
        // Navigate immediately without setState to avoid conflicts
        Navigator.pushReplacementNamed(context, "/home");
        return;
      }

      if (mounted) {
        setState(() {
          loading = false;
        });
      }
      return;
    }
    if (mounted) {
      setState(() {
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isUserValid) {
      return new Container();
    }
    Widget loaderOrButton;
    if (loading) {
      loaderOrButton = new Center(
        child: WidgetUtil.spinLoader(),
      );
    } else {
      loaderOrButton = Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.teal,
                foregroundColor: Colors.white,
                padding: EdgeInsets.symmetric(vertical: 20.0),
              ),
              onPressed: () {
                Navigator.pushNamed(context, '/login');
              },
              child: Text(
                'Login',
                style: TextStyle(fontSize: 18.0),
              ),
            ),
          ),
        ],
      );
    }
    return Scaffold(
        appBar: custom.MenuBar(),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: ListView(children: [
            Padding(
              padding:
                  const EdgeInsets.symmetric(horizontal: 50.0, vertical: 40.0),
              child: Center(
                  child: Image.asset(
                "assets/images/expense-icon.png",
                fit: BoxFit.scaleDown,
              )),
            ),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(
                "Trako",
                style: TextStyle(fontSize: 30.0, fontWeight: FontWeight.w800),
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
