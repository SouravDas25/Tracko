import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

class HorizontalScrollContainer extends StatelessWidget {
  final ScrollController controller;
  final double width;
  final double height;
  final Widget child;

  const HorizontalScrollContainer({
    super.key,
    required this.controller,
    required this.width,
    required this.height,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: ScrollConfiguration(
        behavior: ScrollConfiguration.of(context).copyWith(
          dragDevices: {
            PointerDeviceKind.touch,
            PointerDeviceKind.mouse,
            PointerDeviceKind.trackpad,
          },
        ),
        child: Listener(
          onPointerSignal: (event) {
            if (event is PointerScrollEvent) {
              if (!controller.hasClients) return;
              final delta = event.scrollDelta.dx != 0
                  ? event.scrollDelta.dx
                  : event.scrollDelta.dy;
              final next = (controller.offset + delta)
                  .clamp(0.0, controller.position.maxScrollExtent);
              controller.jumpTo(next);
            }
          },
          child: Scrollbar(
            controller: controller,
            thumbVisibility: true,
            child: SingleChildScrollView(
              controller: controller,
              scrollDirection: Axis.horizontal,
              child: SizedBox(
                width: width,
                height: height,
                child: child,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
