import 'package:expense_manager/component/NumberPad.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:expense_manager/component/screen.dart';

class PinWidget extends StatefulWidget {
  final int length;
  final String phoneNumber;

  PinWidget({this.length = 4, this.phoneNumber = ""}) : super() {
    assert(this.length > 0);
  }

  @override
  State<StatefulWidget> createState() {
    return _PinState(length);
  }
}

class _PinState extends State<PinWidget> {
  final int length;
  List<TextEditingController> controllers = new List<TextEditingController>();
  int count = 0;

  _PinState(this.length) : super() {
    for (int i = 0; i < length; i++) {
      controllers.add(new TextEditingController());
    }
  }

  @override
  void dispose() {
    super.dispose();
    for (TextEditingController controller in controllers) {
      controller.dispose();
    }
  }

  @override
  Widget build(BuildContext context) {
    List<Widget> widgetList = [
      Padding(
        padding: EdgeInsets.only(left: 0.0, right: 2.0),
        child: new Container(
          color: Colors.transparent,
        ),
      ),
      Padding(
        padding: EdgeInsets.only(left: 2.0, right: 0.0),
        child: new Container(
          color: Colors.transparent,
        ),
      ),
    ];

    for (TextEditingController controller in controllers) {
      widgetList.insert(
          1,
          Padding(
            padding: const EdgeInsets.only(right: 2.0, left: 2.0),
            child: new Container(
                alignment: Alignment.center,
                decoration: new BoxDecoration(
                    color: Color.fromRGBO(0, 0, 0, 0.1),
                    border: new Border.all(
                        width: 1.0, color: Color.fromRGBO(0, 0, 0, 0.1)),
                    borderRadius: new BorderRadius.circular(4.0)),
                child: new TextField(
                  inputFormatters: [
                    LengthLimitingTextInputFormatter(1),
                  ],
                  enabled: false,
                  controller: controller,
                  autofocus: false,
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 24.0, color: Colors.black),
                )),
          ));
    }

    widgetList = widgetList.reversed.toList();

    return new Screen(
        body: Container(
      child: Column(
        children: <Widget>[
          Flexible(
            child: Column(
              mainAxisSize: MainAxisSize.max,
              mainAxisAlignment: MainAxisAlignment.start,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    "Verifying your number!",
                    style:
                        TextStyle(fontSize: 18.0, fontWeight: FontWeight.bold),
                  ),
                ),
                Padding(
                  padding:
                      const EdgeInsets.only(left: 16.0, top: 4.0, right: 16.0),
                  child: Text(
                    "Please type the verification code sent to",
                    style: TextStyle(
                        fontSize: 15.0, fontWeight: FontWeight.normal),
                    textAlign: TextAlign.center,
                  ),
                ),
                Padding(
                  padding:
                      const EdgeInsets.only(left: 30.0, top: 2.0, right: 30.0),
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
                )
              ],
            ),
            flex: 90,
          ),
          Flexible(
            child: Column(
                mainAxisSize: MainAxisSize.max,
                mainAxisAlignment: MainAxisAlignment.start,
                children: <Widget>[
                  GridView.count(
                      crossAxisCount: 8,
                      mainAxisSpacing: 10.0,
                      shrinkWrap: true,
                      primary: false,
                      scrollDirection: Axis.vertical,
                      children: List<Container>.generate(widgetList.length,
                          (int index) => Container(child: widgetList[index]))),
                ]),
            flex: 20,
          ),
          NumberPad(
            deleteTextFunction: this.deleteText,
            doneFunction: this.matchOtp,
            insertTextFunction: this.inputTextToField,
          ),
        ],
      ),
    ));
  }

  void inputTextToField(String str) {
    int index = count % controllers.length;
    controllers[index].text = str;
    count++;
  }

  void deleteText() {
    if (count > 0) {
      int index = count--;
      controllers[index].text = "";
    }
  }

  void matchOtp() {
    showDialog(
        context: context,
        builder: (BuildContext context) {
          return AlertDialog(
            title: Text("Successfully"),
            content: Text("Otp matched successfully."),
            actions: <Widget>[
              IconButton(
                  icon: Icon(Icons.check),
                  onPressed: () {
                    Navigator.of(context).pop();
                  })
            ],
          );
        });
  }
}
