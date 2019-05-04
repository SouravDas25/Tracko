import 'package:expense_manager/component/FLushDialog.dart';
import 'package:expense_manager/component/PinWidget.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/models/user.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

class OtpPage extends StatefulWidget {
  final String phoneNumber;

  const OtpPage({Key key, this.phoneNumber}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    // TODO: implement createState
    return _OtpPage();
  }
}

class _OtpPage extends State<OtpPage> {
  String verificationId;
  TextEditingController otpController = new TextEditingController();

  _OtpPage() : super();

  @override
  void initState() {
    super.initState();
    _sendCodeToPhoneNumber();
  }

  redirectToSetUp() {
    Navigator.pop(context);
    Navigator.pushReplacementNamed(context, '/set_up');
  }

  Future<void> _sendCodeToPhoneNumber() async {
//    try {
      final PhoneVerificationCompleted verificationCompleted =
          (FirebaseUser user) {
        print(
            'Inside _sendCodeToPhoneNumber: signInWithPhoneNumber auto succeeded: $user');
        UserBean.createCurrentUser(user.phoneNumber);
        redirectToSetUp();
      };

      final PhoneVerificationFailed verificationFailed =
          (AuthException authException) {
        FlushDialog.flash(context, "Verification Failed",
            'Phone number verification failed. Code: ${authException.code}. Message: ${authException.message}');
      };

      final PhoneCodeSent codeSent =
          (String verificationId, [int forceResendingToken]) async {
        this.verificationId = verificationId;
//    print("code sent to " + widget.phoneNumber);
        FlushDialog.flash(context, "OTP Sent",
            "Your One Time Password has been sent to you phone.");
      };

      final PhoneCodeAutoRetrievalTimeout codeAutoRetrievalTimeout =
          (String verificationId) {
        this.verificationId = verificationId;
        print("time out");
        FlushDialog.flash(
            context, "Verification Timeout", "Your OTP retrival timeout.");
      };

      print(FirebaseAuth.instance);
      await FirebaseAuth.instance.verifyPhoneNumber(
          phoneNumber: '+91' + widget.phoneNumber,
          verificationCompleted: verificationCompleted,
          timeout: const Duration(seconds: 5),
          verificationFailed: verificationFailed,
          codeSent: codeSent,
          codeAutoRetrievalTimeout: codeAutoRetrievalTimeout);

//    } catch (exception) {
//      print(exception.toString());
//      Navigator.pop(context);
//      FlushDialog.flash(context, "Verification Failed", exception.toString());
//    }
  }

  void _signInWithPhoneNumber() async {
    try {
      final AuthCredential credential = PhoneAuthProvider.getCredential(
        verificationId: this.verificationId,
        smsCode: otpController.text,
      );
      final FirebaseUser user =
          await FirebaseAuth.instance.signInWithCredential(credential);
      final FirebaseUser currentUser =
          await FirebaseAuth.instance.currentUser();
      assert(user.uid == currentUser.uid);
      String _message;
      if (user != null) {
        _message = 'Successfully signed in, uid: ' + user.uid;
        redirectToSetUp();
      } else {
        _message = 'Sign in failed';
        FlushDialog.flash(context, "Verification Failed",
            "Not a correct OTP or Server Error.");
      }
      print(_message);
    } catch (exception) {
      Navigator.pop(context);
      FlushDialog.flash(context, "Verification Failed", exception.toString());
    }
  }

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
              "+91 ${widget.phoneNumber}",
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
            maxLength: 6,
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
            padding: EdgeInsets.all(30.0),
            child: RaisedButton(
              color: Theme.of(context).primaryColor,
              textColor: Colors.white,
              onPressed: () {
                this._signInWithPhoneNumber();
//                redirectToSetUp();
              },
              padding: EdgeInsets.symmetric(vertical: 20.0),
              child: Text('Next'),
            ),
          )
        ],
      ),
    );
  }
}
