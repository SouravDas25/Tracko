import 'package:tracko/component/screen.dart';
import 'package:tracko/pages/phone_login_page/otp_page.dart';
import 'package:flutter/material.dart';

class PhoneLoginPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _PhoneLoginPage();
  }
}

class _PhoneLoginPage extends State<PhoneLoginPage> {
  TextEditingController phoneNumberController = new TextEditingController();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Screen(
      titleName: "Verify Phone Number",
      body: Padding(
          padding: EdgeInsets.symmetric(horizontal: 20.0),
          child: ListView(
            children: <Widget>[
              Padding(
                padding: EdgeInsets.all(30.0),
                child: Image.asset(
                  "assets/images/phone_img.png",
                  scale: 1,
                ),
              ),
              Row(
                children: <Widget>[
                  Flexible(
                    flex: 2,
                    child: Text(
                      "+91 ",
                      style:
                      TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    ),
                  ),
                  Flexible(
                    flex: 9,
                    child: TextFormField(
                      maxLength: 10,
                      controller: phoneNumberController,
                      keyboardType: TextInputType.phone,
                      style:
                      TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                      decoration:
                      new InputDecoration(hintText: "Mobile Number"),
                      textAlign: TextAlign.center,
                      validator: (value) {
                        if (value?.isEmpty ?? true) {
                          return 'Please enter some text';
                        }
                        return null;
                      },
                    ),
                  ),
                ],
              ),
              Padding(
                padding: EdgeInsets.all(30.0),
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.teal,
                    foregroundColor: Colors.white,
                    padding: EdgeInsets.symmetric(vertical: 20.0),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) =>
                            OtpPage(phoneNumber: phoneNumberController.text),
                      ),
                    );
                  },
                  child: Text(
                    'Verify',
                    style: TextStyle(fontSize: 20.0),
                  ),
                ),
              )
            ],
          )),
    );
  }
}
