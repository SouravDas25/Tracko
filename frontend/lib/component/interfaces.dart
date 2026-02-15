import 'package:tracko/component/AsynLoadState.dart';
import 'package:flutter/material.dart';

abstract class RefreshableState<T extends StatefulWidget>
    extends AsyncLoadState<T> {
  void refresh();
}
