import 'package:flutter/material.dart';

class PageWidget extends StatelessWidget {
  final int initialPage;
  final Function? onChange;
  final int totalPage;
  final String? title;
  final VoidCallback? onBack;
  final VoidCallback? onNext;
  final bool? disableBack;
  final bool? disableNext;

  PageWidget({
    this.initialPage = 0,
    this.totalPage = 0,
    this.onChange,
    this.title,
    this.onBack,
    this.onNext,
    this.disableBack,
    this.disableNext,
  });

  Widget backButton(context, {disable = false}) {
    return IgnorePointer(
      ignoring: disable,
      child: IconButton(
        icon: Icon(Icons.arrow_back),
        color: disable
            ? Theme.of(context).disabledColor
            : Theme.of(context).primaryColor,
        onPressed: () {
          if (onBack != null) {
            onBack!.call();
            return;
          }
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
          if (onNext != null) {
            onNext!.call();
            return;
          }
          this.onChange?.call(context, this.initialPage + 1);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bool isBackDisabled = disableBack ?? (initialPage <= 1);
    final bool isNextDisabled = disableNext ?? (initialPage >= totalPage);
    final String displayTitle = title ?? "${initialPage} of $totalPage";

    return Card(
      child: ListTile(
        leading: backButton(context, disable: isBackDisabled),
        title: Text(
          displayTitle,
          textAlign: TextAlign.center,
        ),
        trailing: nextButton(context, disable: isNextDisabled),
      ),
    );
  }
}
