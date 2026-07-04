import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:tracko/main.dart' as app;

/// End-to-end tests that drive the real UI against a **live backend**.
///
/// These are meant to run via `frontend/run_e2e_test.py`, which boots the
/// Spring backend with the `test` profile (H2 in-memory, port 8080) and its
/// [GlobalStartupSeeder]. That seeder creates the login `user@example.com` /
/// `password`, a default "Cash" account, and the default categories (FOOD,
/// INCOME, TRANSFER, …). On **web** the app auto-configures its base URL to
/// http://localhost:8080, so no setup is needed inside the tests.
///
/// Run directly (backend + chromedriver already up):
///   flutter drive \
///     --driver=integration_test/test_driver.dart \
///     --target=integration_test/app_test.dart \
///     -d chrome
const String seedEmail = 'user@example.com';
const String seedPassword = 'password';

final Finder _homeFinder = find.byKey(const Key('home_scaffold'));
final Finder _emailFinder = find.byKey(const Key('login_email'));

/// Pumps frames for [duration] without asserting the tree has settled.
///
/// We deliberately avoid [WidgetTester.pumpAndSettle] throughout this suite:
/// the app shows indeterminate progress indicators (session check, save
/// loading dialog) that never "settle", which would make pumpAndSettle hang.
Future<void> pumpFor(WidgetTester tester, Duration duration) async {
  final end = DateTime.now().add(duration);
  while (DateTime.now().isBefore(end)) {
    await tester.pump(const Duration(milliseconds: 100));
  }
}

/// Polls (via [pumpFor]-style pumping) until [finder] matches or [timeout].
Future<void> waitFor(
  WidgetTester tester,
  Finder finder, {
  Duration timeout = const Duration(seconds: 30),
}) async {
  final end = DateTime.now().add(timeout);
  while (finder.evaluate().isEmpty) {
    if (DateTime.now().isAfter(end)) {
      throw TestFailure('Timed out waiting for: ${finder.description}');
    }
    await tester.pump(const Duration(milliseconds: 100));
  }
}

/// Boots the app and ensures we land on the home screen.
///
/// The first test starts with no token and logs in through the form. Later
/// tests reuse the persisted session (web SharedPreferences), so the app
/// auto-navigates to home — we grace-wait for that before deciding to log in
/// manually, which avoids racing the auto-login.
Future<void> bootToHome(WidgetTester tester) async {
  // main() is declared `void main() async`, so it can't be awaited. The
  // pump-loops below give its async bootstrap (DI, InitializeApp) time to run
  // before we query the widget tree.
  app.main();
  await tester.pump();

  final graceEnd = DateTime.now().add(const Duration(seconds: 8));
  while (_homeFinder.evaluate().isEmpty && DateTime.now().isBefore(graceEnd)) {
    await tester.pump(const Duration(milliseconds: 100));
  }

  if (_homeFinder.evaluate().isEmpty) {
    // Not already authenticated → drive the login form.
    await waitFor(tester, _emailFinder, timeout: const Duration(seconds: 20));
    await tester.enterText(_emailFinder, seedEmail);
    await tester.enterText(
        find.byKey(const Key('login_password')), seedPassword);
    await tester.pump();
    await tester.tap(find.byKey(const Key('login_submit')));
    await waitFor(tester, _homeFinder, timeout: const Duration(seconds: 40));
  }
}

/// Opens an [AppBottomSheetPicker] by key and taps the option whose label is
/// [optionText]. Retries because the picker's item list is populated from the
/// backend asynchronously — if the option isn't there yet, we dismiss the
/// sheet (tap the barrier) and try again until [timeout].
Future<void> selectFromPicker(
  WidgetTester tester,
  Key pickerKey,
  String optionText, {
  Duration timeout = const Duration(seconds: 30),
}) async {
  final picker = find.byKey(pickerKey);
  final end = DateTime.now().add(timeout);
  while (true) {
    await waitFor(tester, picker);
    await tester.ensureVisible(picker);
    await tester.pump(const Duration(milliseconds: 100));
    await tester.tap(picker);
    await pumpFor(tester, const Duration(milliseconds: 700));

    // Sheet items are ListTiles containing the label text.
    final option = find.widgetWithText(ListTile, optionText);
    if (option.evaluate().isNotEmpty) {
      await tester.tap(option.last);
      await pumpFor(tester, const Duration(milliseconds: 600));
      return;
    }

    // Items not loaded yet — dismiss the modal barrier and retry.
    await tester.tapAt(const Offset(5, 5));
    await pumpFor(tester, const Duration(milliseconds: 400));
    if (DateTime.now().isAfter(end)) {
      throw TestFailure(
          'Option "$optionText" not found in picker "$pickerKey" within $timeout');
    }
  }
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Tracko live-backend E2E', () {
    testWidgets('logs in with seeded credentials and reaches home',
        (WidgetTester tester) async {
      await bootToHome(tester);
      expect(_homeFinder, findsOneWidget);
    });

    testWidgets('adds a transaction against the live backend',
        (WidgetTester tester) async {
      // A tall, narrow viewport keeps the form's fields on-screen (no rail).
      tester.view.physicalSize = const Size(800, 2000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await bootToHome(tester);

      // Open the New Transaction page from the home app bar.
      final addButton = find.byKey(const Key('home_add_button'));
      await waitFor(tester, addButton);
      await tester.tap(addButton);
      await waitFor(tester, find.byKey(const Key('add_amount_field')),
          timeout: const Duration(seconds: 20));

      // Amount + description. enterText doesn't require the field to be
      // scrolled into view, so these are safe to set directly.
      await tester.enterText(find.byKey(const Key('add_amount_field')), '10000');
      final description = 'E2E Coffee ${DateTime.now().millisecondsSinceEpoch}';
      await tester.enterText(
          find.byKey(const Key('add_description_field')), description);
      await tester.pump();

      // Default type is DEBIT → category picker shows EXPENSE categories.
      await selectFromPicker(tester, const Key('add_category_picker'), 'FOOD');
      await selectFromPicker(tester, const Key('add_account_picker'), 'Cash');

      // Save → real POST /api/transactions.
      final saveButton = find.byKey(const Key('add_save_button'));
      await tester.ensureVisible(saveButton);
      await tester.pump();
      await tester.tap(saveButton);

      // On success the page pops back to home and flashes "Saved"; on a backend
      // failure it stays on the form with an error dialog. So arriving back on
      // home is a reliable signal that the write round-tripped successfully.
      await waitFor(tester, _homeFinder, timeout: const Duration(seconds: 40));
      expect(_homeFinder, findsOneWidget);
    });
  });
}
