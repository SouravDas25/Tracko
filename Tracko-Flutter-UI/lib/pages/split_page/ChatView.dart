import 'dart:async';

import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/ChatMessageTile.dart';
import 'package:tracko/models/chats.dart';
import 'package:tracko/models/user.dart';
import 'package:flutter/material.dart';

class ChatView extends StatefulWidget {
  final User currentUser;
  final User otherUser;
  final Chat currentChat;

  ChatView(this.otherUser, this.currentChat, this.currentUser);

  @override
  State<StatefulWidget> createState() {
    return _ChatList();
  }
}

class _ChatList extends AsyncLoadState<ChatView> {
  final TextEditingController _chatController = new TextEditingController();
  List<dynamic> _messages = <dynamic>[];
  ScrollController scrollController = ScrollController();
  Timer? timer;

  @override
  void initState() {
    super.initState();
    this.timer = Timer.periodic(Duration(seconds: 3), (Timer timer) {
      loadMessages();
    });
//    print("initState : " + widget.currentUser.toString());
  }

  @override
  void didUpdateWidget(ChatView oldWidget) {
    super.didUpdateWidget(oldWidget);
//    loadMessages();
//    print("didUpdateWidget : " + widget.currentUser.toString());
  }

  @override
  void dispose() {
    this.timer?.cancel();
    super.dispose();
  }

  asyncLoad() async {
//    widget.currentUser = await UserController.getCurrentUser();
//    print("asyncLoad : " + widget.currentUser.toString());
    await loadMessages();
    this.loadCompleteView();
  }

  loadMessages() async {
    try {
      final m = await ServerUtil.getChatMessages(
          widget.currentChat.chatGroupId, widget.currentUser.globalId);
      final newMessages = List<dynamic>.from(m ?? const []);
      if (newMessages.length != _messages.length) {
        _messages = newMessages;
        setState(() {});
        scrollList();
      }
    } catch (e) {}
  }

  scrollList() {
    Future.delayed(Duration(milliseconds: 20), () {
      if (scrollController.hasClients)
        scrollController.jumpTo(scrollController.position.maxScrollExtent);
      else {
        scrollList();
      }
    });
  }

  void _handleSubmit(String text) async {
    String message = text;
    _chatController.clear();
    bool sent = await ServerUtil.sendMessage(
        widget.currentUser.globalId, widget.currentChat.chatGroupId, message);
    setState(() {
      _messages.add({
        "message": message,
        "createdAt": new DateTime.now(),
        "userId": widget.currentUser.globalId,
        "sent": sent
      });
    });
    scrollList();
  }

  Widget _chatEnvironment() {
    return IconTheme(
      data: new IconThemeData(color: Theme.of(context).colorScheme.primary),
      child: new Container(
        margin: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 8.0),
        child: new Row(
          children: <Widget>[
            new Flexible(
              child: new TextField(
                  decoration: new InputDecoration(
                    hintText: "Start typing ...",
                    filled: true,
                    fillColor: Theme.of(context).cardColor,
                    contentPadding:
                        EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(24),
                      borderSide: BorderSide.none,
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(24),
                      borderSide: BorderSide(
                          color:
                              Theme.of(context).dividerColor.withOpacity(0.1)),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(24),
                      borderSide: BorderSide(
                          color: Theme.of(context).primaryColor, width: 2),
                    ),
                  ),
                  controller: _chatController,
                  onSubmitted: _handleSubmit,
                  style: TextStyle(fontSize: 16.0)),
            ),
            new Container(
              margin: const EdgeInsets.symmetric(horizontal: 4.0),
              child: new IconButton(
                icon: new Icon(Icons.send),
                color: Theme.of(context).colorScheme.primary,
                onPressed: () => _handleSubmit(_chatController.text),
              ),
            )
          ],
        ),
      ),
    );
  }

  @override
  Widget completeWidget(BuildContext context) {
    return new Column(
      children: <Widget>[
        new Flexible(
          child: _messages.length <= 0
              ? Center(
                  child: Text("No previous messages found"),
                )
              : ListView.builder(
                  padding: new EdgeInsets.all(8.0),
                  reverse: false,
                  itemBuilder: (_, int index) {
                    return ChatMessageTile(
                        _messages[index], widget.currentUser.globalId);
                  },
                  itemCount: _messages.length,
                  controller: scrollController,
                ),
        ),
        new Divider(
          height: 1.0,
        ),
        new Container(
          decoration: new BoxDecoration(
            color: Theme.of(context).cardColor,
          ),
          child: _chatEnvironment(),
        )
      ],
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Chat Data Available."),
    );
  }
}
