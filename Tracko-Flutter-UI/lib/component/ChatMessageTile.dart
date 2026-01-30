import 'package:Tracko/Utils/CommonUtil.dart';
import 'package:flutter/material.dart';

class ChatMessageTile extends StatelessWidget {
  var messageJson;
  String currentUserGlobalId;
  bool isSelf;

  ChatMessageTile(this.messageJson, this.currentUserGlobalId) {
    this.isSelf = this.currentUserGlobalId != this.messageJson['userId'];
  }

  @override
  Widget build(BuildContext context) {
    return Flex(
      direction: Axis.horizontal,
      mainAxisAlignment:
      this.isSelf ? MainAxisAlignment.start : MainAxisAlignment.end,
      children: <Widget>[
        Card(
          elevation: 1.5,
          child: Container(
            constraints: BoxConstraints(minWidth: 50, maxWidth: 300.0),
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                crossAxisAlignment: this.isSelf
                    ? CrossAxisAlignment.start
                    : CrossAxisAlignment.end,
                children: <Widget>[
//                  Padding(
//                    padding: const EdgeInsets.only(bottom: 8.0),
//                    child: Text("Sourav Das",style: TextStyle(
//                      fontSize: 12.0
//                    ),),
//                  ),
                  Text(this.messageJson['message'],
                      textAlign: TextAlign.end,
                      style: TextStyle(
                        fontSize: 18.0,
                        fontWeight: FontWeight.w600,
                      )),
                  Padding(
                    padding: const EdgeInsets.only(top: 8.0),
                    child: Text(
                      CommonUtil.humanDate(
                          this.messageJson['createdAt'] != null
                              ? this.messageJson['createdAt']
                              : DateTime.now(),
                          format: "dd-MM-yyyy"),
                      style: TextStyle(fontSize: 12.0),
                    ),
                  )
                ],
              ),
            ),
          ),
        )
      ],
    );
  }
}
