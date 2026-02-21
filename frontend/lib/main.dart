import 'package:tracko/Init.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/pages/backend_setup_page/backend_setup_page.dart';
import 'package:tracko/pages/login_page/login_page.dart';
import 'package:tracko/pages/route.dart';
import 'package:tracko/pages/transaction_list_page/transaction_list_page.dart';
import 'package:flutter/material.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await InitializeApp.initialize();
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Trako',
      routes: Routes.routes,
      theme: ThemeData(
        brightness: Brightness.light,
        primarySwatch: Colors.blue,
        scaffoldBackgroundColor: Colors.white,
        cardTheme: CardThemeData(
          elevation: 2,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.blue,
        scaffoldBackgroundColor: const Color(0xFF121212),
        cardTheme: CardThemeData(
          elevation: 2,
          color: const Color(0xFF1E1E1E),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF1E1E1E),
          elevation: 0,
        ),
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: Color(0xFF1E1E1E),
        ),
      ),
      themeMode: ThemeMode.dark,
      home: ApiConfig.isConfigured ? LoginPage() : const BackendSetupPage(),
    );
  }
}
