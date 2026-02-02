import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/controllers/SplitController.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/models/chats.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/pages/user_split_view/SplitByUser.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';

class UserListTile extends StatefulWidget {
  final Chat chat;

  UserListTile(this.chat);

  @override
  State<StatefulWidget> createState() {
    return _ChatTile();
  }
}

class _ChatTile extends AsyncLoadState<UserListTile> {
  double sumAmount = 0.0;
  late User otherUser, currentUser;

  @override
  asyncLoad() async {
    this.currentUser = SessionService.currentUser();
    if (widget.chat.userGlobalId.isNotEmpty) {
      this.otherUser = await UserController.findByGlobalId(widget.chat.userGlobalId);
    } else {
      this.otherUser = await UserController.findById(widget.chat.userId);
    }
    final otherUserKey = this.otherUser.globalId.isNotEmpty
        ? this.otherUser.globalId
        : (this.otherUser.id?.toString() ?? '');
    this.sumAmount = await SplitController.getDueAmountByUserId(otherUserKey);
    this.loadCompleteView();
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Card(
      child: ListTile(title: Text("No Chat Data Available")),
    );
  }

  @override
  Widget completeWidget(BuildContext context) {
    return Card(
      child: ListTile(
        onTap: () {
          Navigator.of(context).push(MaterialPageRoute(
              builder: (context) =>
                  SplitByUser(
                      this.otherUser, widget.chat, this.currentUser)));
        },
        title: Text(
          this.otherUser.name,
          style: TextStyle(fontSize: 22.0),
        ),
        subtitle: Text(CommonUtil.humanDate(DateTime.now())),
        trailing: WidgetUtil.transformAmount2TextWidget(
            TransactionType.DEBIT, this.sumAmount,
            addSign: false),
        leading: CircleAvatar(
          radius: 30.0,
          child: Image.asset("assets/images/splitavatar.png"),
        ),
        contentPadding: EdgeInsets.all(8.0),
      ),
    );
  }
}
