class SmartUtil {
  static bool _isAutoEnable = false;

  static void setAutoEnable() {
    _isAutoEnable = true;
  }

  static bool isAutoEnable() {
    bool b = _isAutoEnable;
    _isAutoEnable = false;
    return b;
  }
}
