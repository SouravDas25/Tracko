import 'package:Tracko/component/interfaces.dart';
import 'package:Tracko/controllers/ChatController.dart';
import 'package:Tracko/models/chats.dart';
import 'package:Tracko/pages/split_page/UserListTile.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class SplitPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _SplitPage();
  }
}

class _SplitPage extends RefreshableState<SplitPage> {
  RefreshController refreshController = new RefreshController();
  List<Chat> chats = [];

  _SplitPage();

  @override
  void didUpdateWidget(SplitPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    asyncLoad();
//    print("didUpdateWidget");
  }

  void asyncLoad() async {
    refresh(); // Note: refresh() returns void
//    print(widget.chats);
    if (this.chats.length <= 0)
      this.loadFallbackView();
    else
      this.loadCompleteView();
  }

  @override
  void refresh() async {
    this.chats = await ChatController.getAllSharedTransactions();
    if (this.mounted)
      setState(() {
        refreshController.refreshCompleted();
      });
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Splits Available"),
    );
  }

  @override
  Widget completeWidget(BuildContext context) {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: () {
        refresh();
      },
      child: ListView.builder(
        itemBuilder: (_, int index) {
          return UserListTile(this.chats[index]);
        },
        itemCount: this.chats.length,
      ),
    );
  }


}
