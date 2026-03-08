import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HorizontalScrollContainer extends StatelessWidget {
  final ScrollController controller;
  final double width;
  final double height;
  final Widget child;
  // Called with scroll delta when Ctrl+scroll is detected (for zoom).
  final void Function(double delta)? onCtrlScroll;

  const HorizontalScrollContainer({
    super.key,
    required this.controller,
    required this.width,
    required this.height,
    required this.child,
    this.onCtrlScroll,
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
        child: Scrollbar(
          controller: controller,
          thumbVisibility: true,
          child: SingleChildScrollView(
            controller: controller,
            scrollDirection: Axis.horizontal,
            child: SizedBox(
              width: width,
              height: height,
              // Listener is inside SingleChildScrollView so it registers
              // with GestureBinding.pointerSignalResolver BEFORE the scroll
              // view, giving it priority for every PointerScrollEvent.
              child: Listener(
                onPointerSignal: (event) {
                  if (event is PointerScrollEvent) {
                    final isCtrl = HardwareKeyboard.instance.isControlPressed ||
                        HardwareKeyboard.instance.isMetaPressed;
                    if (isCtrl && onCtrlScroll != null) {
                      final delta = event.scrollDelta.dy != 0
                          ? event.scrollDelta.dy
                          : event.scrollDelta.dx;
                      onCtrlScroll!(delta);
                      return;
                    }
                    // Normal scroll: manually drive the controller so the
                    // inner Listener doesn't block SingleChildScrollView.
                    if (!controller.hasClients) return;
                    final delta = event.scrollDelta.dx != 0
                        ? event.scrollDelta.dx
                        : event.scrollDelta.dy;
                    final next = (controller.offset + delta)
                        .clamp(0.0, controller.position.maxScrollExtent);
                    controller.jumpTo(next);
                  }
                },
                child: child,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
