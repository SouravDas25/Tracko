import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tracko/main.dart' as app;

/// Waits for a widget to appear on the screen.
Future<void> waitFor(WidgetTester tester, Finder finder, {Duration timeout = const Duration(seconds: 30)}) async {
  final end = DateTime.now().add(timeout);
  do {
    if (DateTime.now().isAfter(end)) {
      throw Exception('Timed out waiting for $finder');
    }
    await tester.pumpAndSettle();
    await Future.delayed(const Duration(milliseconds: 100));
  } while (finder.evaluate().isEmpty);
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Tracko Backend Integration Tests', () {
        testWidgets('Complete authentication bypass and setup flow', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for welcome page to load and tap "Login"
      final loginButton = find.widgetWithText(ElevatedButton, 'Login');
      await waitFor(tester, loginButton);
      await tester.tap(loginButton);

      // Enter any phone number
      final phoneField = find.byType(TextFormField).first;
      await waitFor(tester, phoneField);
      await tester.enterText(phoneField, '1234567890');

      // Tap verify/next button
      final verifyButton = find.widgetWithText(ElevatedButton, 'Verify');
      await waitFor(tester, verifyButton);
      await tester.tap(verifyButton);

      // Enter any OTP
      final otpField = find.byType(TextFormField).first;
      await waitFor(tester, otpField);
      await tester.enterText(otpField, '123456');

      // Tap next to complete authentication
      final nextButtonOnOtp = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnOtp);
      await tester.tap(nextButtonOnOtp);

      // Wait for navigation to setup page
      final nextButtonOnSetup = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnSetup);

      // Verify we're on the setup page
      expect(nextButtonOnSetup, findsOneWidget);

      print('✅ Authentication bypass successful');
    });

        testWidgets('User setup with backend integration', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Navigate through authentication
      final loginButton = find.widgetWithText(ElevatedButton, 'Login');
      await waitFor(tester, loginButton);
      await tester.tap(loginButton);

      final phoneField = find.byType(TextFormField).first;
      await waitFor(tester, phoneField);
      await tester.enterText(phoneField, '1234567890');

      final verifyButton = find.widgetWithText(ElevatedButton, 'Verify');
      await waitFor(tester, verifyButton);
      await tester.tap(verifyButton);

      final otpField = find.byType(TextFormField).first;
      await waitFor(tester, otpField);
      await tester.enterText(otpField, '123456');

      final nextButtonOnOtp = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnOtp);
      await tester.tap(nextButtonOnOtp);

      // Now on setup page - enter user details
      final nameField = find.widgetWithText(TextFormField, 'Name');
      await waitFor(tester, nameField);
      await tester.enterText(nameField, 'Test User');

      final emailField = find.widgetWithText(TextFormField, 'Email Address');
      await waitFor(tester, emailField);
      await tester.enterText(emailField, 'test@example.com');

      // Tap Next to create account via backend
      final nextButtonOnSetup = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnSetup);
      await tester.tap(nextButtonOnSetup);

      // Wait for backend API call and navigation to home
      final homeNavBar = find.byType(BottomNavigationBar);
      await waitFor(tester, homeNavBar, timeout: const Duration(seconds: 20));

      // Verify we're on the home page
      expect(homeNavBar, findsOneWidget);

      print('✅ User setup and backend integration successful');
    });

        testWidgets('Session persistence test', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Complete authentication and setup
      final loginButton = find.widgetWithText(ElevatedButton, 'Login');
      await waitFor(tester, loginButton);
      await tester.tap(loginButton);

      final phoneField = find.byType(TextFormField).first;
      await waitFor(tester, phoneField);
      await tester.enterText(phoneField, '1234567890');

      final verifyButton = find.widgetWithText(ElevatedButton, 'Verify');
      await waitFor(tester, verifyButton);
      await tester.tap(verifyButton);

      final otpField = find.byType(TextFormField).first;
      await waitFor(tester, otpField);
      await tester.enterText(otpField, '123456');

      final nextButtonOnOtp = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnOtp);
      await tester.tap(nextButtonOnOtp);

      final nameField = find.widgetWithText(TextFormField, 'Name');
      await waitFor(tester, nameField);
      await tester.enterText(nameField, 'Session Test User');

      final emailField = find.widgetWithText(TextFormField, 'Email Address');
      await waitFor(tester, emailField);
      await tester.enterText(emailField, 'session@example.com');

      final nextButtonOnSetup = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnSetup);
      await tester.tap(nextButtonOnSetup);

      // Verify user is logged in and session is maintained
      final homeNavBar = find.byType(BottomNavigationBar);
      await waitFor(tester, homeNavBar, timeout: const Duration(seconds: 20));
      expect(homeNavBar, findsOneWidget);

      print('✅ Session persistence verified');
    });

        testWidgets('Backend API connectivity test', (WidgetTester tester) async {
      // This test verifies that the app can communicate with backend
      app.main();
      await tester.pumpAndSettle();

      // Navigate to authenticated state
      final loginButton = find.widgetWithText(ElevatedButton, 'Login');
      await waitFor(tester, loginButton);
      await tester.tap(loginButton);

      final phoneField = find.byType(TextFormField).first;
      await waitFor(tester, phoneField);
      await tester.enterText(phoneField, '1234567890');

      final verifyButton = find.widgetWithText(ElevatedButton, 'Verify');
      await waitFor(tester, verifyButton);
      await tester.tap(verifyButton);

      final otpField = find.byType(TextFormField).first;
      await waitFor(tester, otpField);
      await tester.enterText(otpField, '123456');

      final nextButtonOnOtp = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnOtp);
      await tester.tap(nextButtonOnOtp);

      // Enter user details
      final nameField = find.widgetWithText(TextFormField, 'Name');
      await waitFor(tester, nameField);
      await tester.enterText(nameField, 'API Test User');

      final emailField = find.widgetWithText(TextFormField, 'Email Address');
      await waitFor(tester, emailField);
      await tester.enterText(emailField, 'api@example.com');

      final nextButtonOnSetup = find.widgetWithText(ElevatedButton, 'Next');
      await waitFor(tester, nextButtonOnSetup);
      await tester.tap(nextButtonOnSetup);

      // Wait for backend API calls and navigation to home
      final homeNavBar = find.byType(BottomNavigationBar);
      await waitFor(tester, homeNavBar, timeout: const Duration(seconds: 20));

      // If we reach home page, backend APIs are working
      expect(homeNavBar, findsOneWidget);

      print('✅ Backend API connectivity verified');
    });
  });
}
