import 'package:Tracko/Utils/ServerUtil.dart';
import 'package:Tracko/Utils/WidgetUtil.dart';
import 'package:Tracko/component/FLushDialog.dart';
import 'package:Tracko/component/LoadingDialog.dart';
import 'package:Tracko/component/screen.dart';
import 'package:Tracko/models/user.dart' as TrackoUser;
import 'package:Tracko/services/BackupService.dart';
import 'package:Tracko/services/SessionService.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

class OtpPage extends StatefulWidget {
  final String phoneNumber;

  OtpPage({
    Key? key,
    required this.phoneNumber,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _OtpPage(this.phoneNumber);
  }
}

class _OtpPage extends State<OtpPage> {
  String phoneNumberWithCountryCode = '';
  String verificationId = '';
  TextEditingController otpController = new TextEditingController();

  _OtpPage(String phoneNumber) : super() {
    this.phoneNumberWithCountryCode = '+91' + phoneNumber;
    print(this.phoneNumberWithCountryCode);
  }

  @override
  void initState() {
    super.initState();
    sendOtpToPhoneNumber();
  }

  afterAuthentication(String phoneNumber, [String? uuid]) async {
    await BackupService.restoreDatabaseIfRequired(phoneNumber);
    TrackoUser.User user = await SessionService.createCurrentUser(phoneNumber, uuid: uuid ?? '');
    await ServerUtil.signUp(user);
  }

  redirectToSetUp() {
    Navigator.pop(context);
    Navigator.pushReplacementNamed(context, '/set_up');
  }

  verificationCompleted(AuthCredential credential) async {
    print('sign-in auto succeeded: $credential');
    await authenticate(credential: credential);
  }

  verificationFailed(FirebaseAuthException authException) {
    print("This is a Auth Exception : " + (authException.message ?? 'Unknown error'));
    if (this.mounted)
      FlushDialog.flash(context, "Verification Failed",
          'Phone number verification failed. Code: ${authException
              .code}. Message: ${authException.message}');
  }

  codeSent(String verificationId, int? forceResendingToken) async {
    this.verificationId = verificationId;
    if (this.mounted) WidgetUtil.toast("OTP Sent");
  }

  codeAutoRetrievalTimeout(String verificationId) {
    this.verificationId = verificationId;
    print("time out");
    if (this.mounted) WidgetUtil.toast("Verification Timeout");
  }

  Future<void> sendOtpToPhoneNumber() async {
    try {
//      print(FirebaseAuth.instance);
      await FirebaseAuth.instance.verifyPhoneNumber(
          phoneNumber: this.phoneNumberWithCountryCode,
          verificationCompleted: verificationCompleted,
          timeout: const Duration(seconds: 5),
          verificationFailed: verificationFailed,
          codeSent: codeSent,
          codeAutoRetrievalTimeout: codeAutoRetrievalTimeout);
    } catch (exception) {
      print("This is a custom Exception : " + exception.toString());
      Navigator.pop(context);
      FlushDialog.flash(context, "Verification Failed", exception.toString());
    }
  }

  Future<AuthCredential> submitOtp() async {
    final AuthCredential credential = PhoneAuthProvider.credential(
      verificationId: this.verificationId,
      smsCode: otpController.text,
    );
    return credential;
  }

  Future<void> authenticate({required AuthCredential credential}) async {
    bool isAuthSuccessful = false;
    try {
      LoadingDialog.show(context);
      if (credential == null) {
        credential = await submitOtp();
      }
      final UserCredential authResult =
      await FirebaseAuth.instance.signInWithCredential(credential);
      final User? currentUser =
      FirebaseAuth.instance.currentUser;
      assert(authResult.user?.uid == currentUser?.uid);
      String _message;
      if (authResult.user != null) {
        _message = 'Successfully signed in, uid: ' + (authResult.user?.uid ?? '');
        await afterAuthentication(widget.phoneNumber, authResult.user?.uid);
        isAuthSuccessful = true;
      } else {
        _message = 'Sign in failed';
        if (this.mounted)
          FlushDialog.flash(context, "Verification Failed",
              "Not a correct OTP or Server Error.");
      }
      print(_message);
    } catch (exception) {
      print("This is a after Otp Exception : " + exception.toString());
      Navigator.pop(context);
      if (this.mounted)
        FlushDialog.flash(context, "Verification Failed", exception.toString());
    } finally {
      LoadingDialog.hide(context);
    }
    if (isAuthSuccessful) {
      redirectToSetUp();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Screen(
      titleName: "Verify OTP",
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
            padding:
            const EdgeInsets.symmetric(horizontal: 16.0, vertical: 75.0),
            child: Center(
              child: Image(
                image: AssetImage('assets/images/otp-icon.png'),
                height: 120.0,
                width: 120.0,
              ),
            ),
          ),
          TextFormField(
            maxLength: 6,
            controller: otpController,
            keyboardType: TextInputType.number,
            decoration: new InputDecoration(hintText: "OTP Code"),
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
            textAlign: TextAlign.center,
            validator: (value) {
              if (value?.isEmpty ?? true) {
                return 'Please enter otp';
              }
              return "";
            },
          ),
          Padding(
            padding: EdgeInsets.all(30.0),
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.teal,
                foregroundColor: Colors.white,
                padding: EdgeInsets.symmetric(vertical: 20.0),
              ),
              onPressed: () async {
                await this.authenticate(credential: await submitOtp());
              },
              child: Text(
                'Next',
                style: TextStyle(fontSize: 18.0),
              ),
            ),
          )
        ],
      ),
    );
  }
}
