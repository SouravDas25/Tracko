
import 'package:expense_manager/component/screen.dart';
import 'package:flutter/material.dart';

class SetUpPage extends StatefulWidget {

  @override
  State<StatefulWidget> createState() {
    return _setUpPage();
  }

}

class _setUpPage extends State<SetUpPage>{

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Screen(
      body: ListView(
        children: <Widget>[
          CircleAvatar(
            child: Image.asset('assets/images/user-avatar.png'),
          )
        ],
      ),
    );
  }

}