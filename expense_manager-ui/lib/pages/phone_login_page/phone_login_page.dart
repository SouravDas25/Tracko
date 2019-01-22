import 'package:flutter/material.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/pages/phone_login_page/otp_page.dart';

class PhoneLoginPage extends StatelessWidget {

  TextEditingController phoneNumberController = new TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Screen(
      body: Padding(
          padding: EdgeInsets.symmetric(horizontal: 20.0),
          child: ListView(
            children: <Widget>[
              Image.asset("assets/images/phone_img.png"),
              TextFormField(
                maxLength: 10,
                controller: phoneNumberController,
                keyboardType: TextInputType.number,
                decoration: new InputDecoration(hintText: "Mobile Number"),
                textAlign: TextAlign.center,
                validator: (value) {
                  if (value.isEmpty) {
                    return 'Please enter some text';
                  }
                },
              ),
              Padding(
                padding:
                    EdgeInsets.symmetric(horizontal: 100.0, vertical: 20.0),
                child: RaisedButton(
                  color: Theme.of(context).primaryColor,
                  textColor: Colors.white,
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => OtpPage(phoneNumberController.text)),
                    );
                  },
                  padding: EdgeInsets.symmetric(vertical: 20.0),
                  child: Text('Verify'),
                ),
              )
            ],
          )),
    );
  }
}
