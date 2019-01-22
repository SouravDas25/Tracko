import 'package:shared_preferences/shared_preferences.dart';


class Settings {

  static getStore() {
    return SharedPreferences.getInstance();
  }


}