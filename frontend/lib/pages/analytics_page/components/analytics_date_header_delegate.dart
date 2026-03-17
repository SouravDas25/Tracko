import 'package:flutter/material.dart';

class AnalyticsDateHeaderDelegate extends SliverPersistentHeaderDelegate {
  final String dateText;
  final bool isLoading;
  final bool disableNavigation;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  AnalyticsDateHeaderDelegate({
    required this.dateText,
    required this.isLoading,
    this.disableNavigation = false,
    required this.onPrevious,
    required this.onNext,
  });

  @override
  Widget build(
      BuildContext context, double shrinkOffset, bool overlapsContent) {
    return Container(
      color: Theme.of(context).appBarTheme.backgroundColor ??
          Theme.of(context).primaryColor,
      child: SafeArea(
        top: false,
        bottom: false,
        child: SizedBox(
          height: 48.0,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                icon: Icon(
                  Icons.chevron_left_rounded,
                  color: disableNavigation ? Colors.white38 : Colors.white,
                ),
                onPressed: isLoading || disableNavigation ? null : onPrevious,
              ),
              Text(
                dateText,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: Colors.white,
                ),
              ),
              IconButton(
                icon: Icon(
                  Icons.chevron_right_rounded,
                  color: disableNavigation ? Colors.white38 : Colors.white,
                ),
                onPressed: isLoading || disableNavigation ? null : onNext,
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  double get maxExtent => 48.0;

  @override
  double get minExtent => 48.0;

  @override
  bool shouldRebuild(AnalyticsDateHeaderDelegate oldDelegate) {
    return dateText != oldDelegate.dateText ||
        isLoading != oldDelegate.isLoading ||
        disableNavigation != oldDelegate.disableNavigation ||
        onPrevious != oldDelegate.onPrevious ||
        onNext != oldDelegate.onNext;
  }
}
