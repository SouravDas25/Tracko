import 'package:expense_manager/component/PinWidget.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:flutter/material.dart';

class OtpPage extends StatelessWidget {
  final String phoneNumber;

  TextEditingController otpController = new TextEditingController();

  OtpPage(this.phoneNumber) : super();

  @override
  Widget build(BuildContext context) {
    return Screen(
      body: ListView(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Text(
              "Verifying your number!",
              style: TextStyle(fontSize: 18.0, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 16.0, top: 4.0, right: 16.0),
            child: Text(
              "Please type the verification code sent to",
              style: TextStyle(fontSize: 15.0, fontWeight: FontWeight.normal),
              textAlign: TextAlign.center,
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 30.0, top: 2.0, right: 30.0),
            child: Text(
              "+91 ${this.phoneNumber}",
              style: TextStyle(
                  fontSize: 15.0,
                  fontWeight: FontWeight.bold,
                  color: Colors.red),
              textAlign: TextAlign.center,
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(top: 16.0),
            child: Image(
              image: AssetImage('assets/images/otp-icon.png'),
              height: 120.0,
              width: 120.0,
            ),
          ),
          TextFormField(
            maxLength: 4,
            controller: otpController,
            keyboardType: TextInputType.number,
            decoration: new InputDecoration(hintText: "OTP Code"),
            textAlign: TextAlign.center,
            validator: (value) {
              if (value.isEmpty) {
                return 'Please enter otp';
              }
            },
          ),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 100.0, vertical: 20.0),
            child: RaisedButton(
              color: Theme.of(context).primaryColor,
              textColor: Colors.white,
              onPressed: () {
              },
              padding: EdgeInsets.symmetric(vertical: 20.0),
              child: Text('Submit'),
            ),
          )
        ],
      ),
    );
  }
}
