import 'package:Tracko/pages/add_item_page/add_item.dart';
import 'package:Tracko/pages/home_page/home_tab.dart';
import 'package:Tracko/pages/login_page/login_page.dart';
import 'package:Tracko/pages/phone_login_page/phone_login_page.dart';
import 'package:Tracko/pages/set_up_page/set_up_page.dart';
import 'package:Tracko/pages/welcome_page/welcome_page.dart';
import 'package:flutter/material.dart';

class Routes {
  static Map<String, WidgetBuilder> routes = {
    '/set_up': (BuildContext context) => new SetUpPage(),
    '/home': (BuildContext context) => new HomeTab(),
    '/login': (BuildContext context) => new LoginPage(),
    '/phone_login': (BuildContext context) => new PhoneLoginPage(),
    '/welcome': (BuildContext context) => new WelcomePage(),
    '/add_item': (BuildContext context) => new AddItemPage(),
  };
}
