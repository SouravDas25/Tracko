import 'package:expense_manager/pages/add_item_page/add_item.dart';
import 'package:expense_manager/pages/home_page/home_page.dart';
import 'package:expense_manager/pages/login_page/login_page.dart';
import 'package:expense_manager/pages/phone_login_page/phone_login_page.dart';
import 'package:expense_manager/pages/welcome_page/welcome_page.dart';
import 'package:flutter/material.dart';

class Routes {
  static Map<String, WidgetBuilder> routes =  {
    '/home': (BuildContext context) => new HomePage(),
    '/login': (BuildContext context) => new LoginPage(),
    '/phone_login': (BuildContext context) => new PhoneLoginPage(),
    '/welcome' : (BuildContext context) => new WelcomePage(),
    '/add_item' : (BuildContext context) => new AddItemPage(),
  };
}

