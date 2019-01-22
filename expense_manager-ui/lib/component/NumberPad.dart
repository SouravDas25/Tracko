import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class NumberPad extends StatelessWidget {
  final Function deleteTextFunction, insertTextFunction, doneFunction;

  NumberPad({this.deleteTextFunction,this.doneFunction,this.insertTextFunction}) : super();

  @override
  Widget build(BuildContext context) {
    return Flexible(
      child: Column(
        mainAxisSize: MainAxisSize.max,
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          new Container(
            child: Padding(
              padding: const EdgeInsets.only(
                  left: 8.0, top: 16.0, right: 8.0, bottom: 0.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("1");
                    },
                    child: Text("1",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("2");
                    },
                    child: Text("2",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("3");
                    },
                    child: Text("3",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                ],
              ),
            ),
          ),
          new Container(
            child: Padding(
              padding: const EdgeInsets.only(
                  left: 8.0, top: 4.0, right: 8.0, bottom: 0.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("4");
                    },
                    child: Text("4",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("5");
                    },
                    child: Text("5",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("6");
                    },
                    child: Text("6",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                ],
              ),
            ),
          ),
          new Container(
            child: Padding(
              padding: const EdgeInsets.only(
                  left: 8.0, top: 4.0, right: 8.0, bottom: 0.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("7");
                    },
                    child: Text("7",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("8");
                    },
                    child: Text("8",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("9");
                    },
                    child: Text("9",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                ],
              ),
            ),
          ),
          new Container(
            child: Padding(
              padding: const EdgeInsets.only(
                  left: 8.0, top: 4.0, right: 8.0, bottom: 0.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  MaterialButton(
                      onPressed: () {
                        deleteTextFunction();
                      },
                      child: Image.asset('assets/images/delete.png',
                          width: 25.0, height: 25.0)),
                  MaterialButton(
                    onPressed: () {
                      insertTextFunction("0");
                    },
                    child: Text("0",
                        style: TextStyle(
                            fontSize: 25.0, fontWeight: FontWeight.w400),
                        textAlign: TextAlign.center),
                  ),
                  MaterialButton(
                      onPressed: () {
                        doneFunction();
                      },
                      child: Image.asset('assets/images/success.png',
                          width: 25.0, height: 25.0)),
                ],
              ),
            ),
          ),
        ],
      ),
      flex: 90,
    );
  }
}
