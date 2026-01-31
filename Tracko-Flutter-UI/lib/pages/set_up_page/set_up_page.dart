import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/component/screen.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/GlobalAccountResponse.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/services/SessionService.dart';
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
  String globalAccountId = '';
  int initLabel = 0;

  @override
  void initState() {
    super.initState();
    getAutoFillData();
  }

  getAutoFillData() async {
    try {
      User user = await SessionService.getCurrentUser();
      GlobalAccountResponse globalAccount =
      await ServerUtil.getGlobalAccount(user.phoneNo) ?? GlobalAccountResponse.fromJson({});
      print(globalAccount);
      if (globalAccount != null) {
        if (nameController.text.length <= 0 &&
            emailController.text.length <= 0) {
          nameController.text = globalAccount.name;
          emailController.text = globalAccount.email;
        }
        globalAccountId = globalAccount.id;
        initLabel = 1;
      } else
        initLabel = 2;
    } catch (e) {
      initLabel = 2;
    }
    setState(() {});
  }

  updateUser() async {
    try {
      LoadingDialog.show(context);
      
      // Get current user from session
      User user = await SessionService.getCurrentUser();
      
      print(user);
      user.name = nameController.text;
      user.email = emailController.text;
      
      String globalId = this.globalAccountId;
      if (globalId == null || globalId.isEmpty) {
        // Create new global account via backend
        globalId = await ServerUtil.createGlobalAccount(user) ?? '';
        if (globalId == null || globalId.isEmpty) {
          LoadingDialog.hide(context);
          WidgetUtil.toast("Failed to create account. Please try again.");
          Navigator.pushReplacementNamed(context, "/welcome");
          return;
        }
        user.globalId = globalId;
      } else {
        // Update existing global account via backend
        user.globalId = globalId;
        await ServerUtil.updateGlobalUser(user);
      }
      
      // Save user via backend repository
      await UserController.saveUser(user);
      
      // Update session
      SessionService.setCurrentUser(user);

      print(user);
      LoadingDialog.hide(context);
      Navigator.pushReplacementNamed(context, "/home");
    } catch (e) {
      print("sign-in failed in set-up page: ${e.toString()}");
      LoadingDialog.hide(context);
      WidgetUtil.toast("Setup failed: ${e.toString()}");
    }
  }

  @override
  Widget build(BuildContext context) {
    if (initLabel == null) {
      return Screen(
        body: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            WidgetUtil.spinLoader(),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 18.0),
              child: Text(
                "getting you logged in please wait...",
                style: TextStyle(fontWeight: FontWeight.w600),
              ),
            )
          ],
        ),
      );
    }
    return completeBuild(context);
  }

  Widget completeBuild(BuildContext context) {
    return Screen(
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              CircleAvatar(
                radius: 100.0,
                child: Image.asset('assets/images/user-avatar.png'),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                child: Row(
                  children: <Widget>[
                    Flexible(
                      flex: 9,
                      child: TextFormField(
                        controller: nameController,
                        keyboardType: TextInputType.text,
                        decoration: new InputDecoration(hintText: "Name"),
                        style: TextStyle(fontSize: 20.0),
                        validator: (value) {
                          if (value?.isEmpty ?? true) {
                            return 'Please enter some text';
                          }
                          return null;
                        },
                      ),
                    ),
                    Flexible(
                      flex: 1,
                      child: Center(
                        child: initLabel == 1
                            ? Icon(Icons.check)
                            : Icon(Icons.clear),
                      ),
                    )
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                child: Row(
                  children: <Widget>[
                    Flexible(
                      flex: 9,
                      child: TextFormField(
                        controller: emailController,
                        keyboardType: TextInputType.emailAddress,
                        decoration:
                        new InputDecoration(hintText: "Email Address"),
                        style: TextStyle(fontSize: 20.0),
                        validator: (value) {
                          if (value?.isEmpty ?? true) {
                            return 'Please enter some text';
                          }
                          return null;
                        },
                      ),
                    ),
                    Flexible(
                      flex: 1,
                      child: Center(
                        child: initLabel == 1
                            ? Icon(Icons.check)
                            : Icon(Icons.clear),
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(30.0),
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.teal,
                    foregroundColor: Colors.white,
                    padding: EdgeInsets.symmetric(vertical: 20.0),
                  ),
                  onPressed: () {
                    if (_formKey.currentState?.validate() ?? false) {
                      // If the form is valid, display a snackbar. In the real world, you'd
                      // often want to call a server or save the information in a database
                      updateUser();
                    }
                  },
                  child: Text(
                    'Next',
                    style: TextStyle(fontSize: 20.0),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
