import 'package:flutter/foundation.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/di/di.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/Utils/HealthCheckUtil.dart';
import 'package:dio/dio.dart';
import 'package:tracko/services/auth_service.dart';

class InitializeApp {
  static Future initialize() async {
    AppLog.d("InitializeApp: Starting initialization");

    // Load persisted backend URL first
    await ApiConfig.loadBaseUrl();
    AppLog.d(
        "InitializeApp: Base URL loaded: ${ApiConfig.baseUrl}. Configured: ${ApiConfig.isConfigured}");

    // Ensure ApiClient is using the loaded URL
    if (ApiConfig.isConfigured) {
      sl<ApiClient>().updateBaseUrl(ApiConfig.baseUrl);
    }

    // If configured and not web, verify connection
    if (!kIsWeb && ApiConfig.isConfigured) {
      AppLog.d("InitializeApp: Verifying connection to ${ApiConfig.health}");

      final isHealthy = await HealthCheckUtil.checkHealth(ApiConfig.baseUrl);

      if (isHealthy) {
        AppLog.d("InitializeApp: Health check passed");
      } else {
        AppLog.d(
            "InitializeApp: Backend unreachable at ${ApiConfig.baseUrl}. Resetting config.");
        // Store the failed URL so we can pre-fill it for editing
        ApiConfig.lastAttemptedUrl = ApiConfig.baseUrl;
        // Reset config to force BackendSetupPage
        await ApiConfig.reset();
        AppLog.d(
            "InitializeApp: Config reset. New configured state: ${ApiConfig.isConfigured}");
      }
    } else {
      AppLog.d(
          "InitializeApp: Skipping health check (Web: $kIsWeb, Configured: ${ApiConfig.isConfigured})");
    }

    // App is now fully backend-driven
    // Initialize session by fetching user profile
    // Only attempt if configured or on web (since reset() might have cleared it)
    if (ApiConfig.isConfigured) {
      final loggedIn = await sl<AuthService>().isLoggedIn();
      if (loggedIn) {
        AppLog.d("InitializeApp: Fetching current user session");
        try {
          await sl<SessionService>().fetchMe();
        } catch (e) {
          AppLog.d("InitializeApp: Session fetch failed: $e");
        }
      } else {
        AppLog.d("InitializeApp: Skipping session fetch (Not logged in)");
      }
    } else {
      AppLog.d("InitializeApp: Skipping session fetch (Not configured)");
    }

    AppLog.d("InitializeApp: Initialization complete");
  }
}
