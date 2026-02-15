import 'package:tracko/services/SessionService.dart';

class InitializeApp {
  static Future initialize() async {
    // App is now fully backend-driven
    // Initialize session by fetching user profile
    await SessionService.getCurrentUser();
  }
}
