import 'package:flutter/material.dart';

abstract class AsyncLoadState<T extends StatefulWidget> extends State<T> {
  @protected
  bool __isLoading = true;

  @protected
  bool __isError = false;

  @mustCallSuper
  void loadCompleteView() {
    this.__isLoading = false;
    this.__isError = false;
    if (this.mounted) setState(() {});
  }

  @mustCallSuper
  void loadFallbackView() {
    this.__isLoading = false;
    this.__isError = true;
    if (this.mounted) setState(() {});
  }

  Widget __loadingWidget() {
    return Container(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: Center(
        child: CircularProgressIndicator(),
      ),
    );
  }

  @override
  void initState() {
    super.initState();
    this.asyncLoad();
  }

//  @override
//  void didUpdateWidget(Widget oldWidget) {
//    super.didUpdateWidget(oldWidget);
//    __isLoading = true;
//    __isError = false;
//    asyncLoad();
//  }

  asyncLoad();

  Widget fallbackWidget(BuildContext context);

  Widget completeWidget(BuildContext context);

  @override
  Widget build(BuildContext context) {
    if (this.__isLoading == true) {
      return __loadingWidget();
    }
    if (this.__isLoading == false && this.__isError == true) {
      return fallbackWidget(context);
    }
    return completeWidget(context);
  }
}
