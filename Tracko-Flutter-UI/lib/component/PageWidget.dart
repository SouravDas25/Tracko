import 'package:flutter/material.dart';

class PageWidget extends StatelessWidget {
  final int initialPage;
  final Function? onChange;
  final int totalPage;

  PageWidget({this.initialPage = 0, this.totalPage = 0, this.onChange});

  Widget backButton(context, {disable = false}) {
    return IgnorePointer(
      ignoring: disable,
      child: IconButton(
        icon: Icon(Icons.arrow_back),
        color: disable
            ? Theme.of(context).disabledColor
            : Theme.of(context).primaryColor,
        onPressed: () {
          this.onChange?.call(context, this.initialPage - 1);
        },
      ),
    );
  }

  Widget nextButton(context, {disable = false}) {
    return IgnorePointer(
      ignoring: disable,
      child: IconButton(
        icon: Icon(
          Icons.arrow_forward,
          color: disable
              ? Theme.of(context).disabledColor
              : Theme.of(context).primaryColor,
        ),
        onPressed: () {
          this.onChange?.call(context, this.initialPage + 1);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: backButton(context, disable: initialPage <= 1),
        title: Text(
          "${initialPage} of $totalPage",
          textAlign: TextAlign.center,
        ),
        trailing: nextButton(context, disable: initialPage >= totalPage),
      ),
    );
  }
}
