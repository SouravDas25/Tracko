import 'package:flutter/foundation.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:dio/dio.dart';

class InitializeApp {
  static Future initialize() async {
    AppLog.d("InitializeApp: Starting initialization");

    // Load persisted backend URL first
    await ApiConfig.loadBaseUrl();
    AppLog.d(
        "InitializeApp: Base URL loaded: ${ApiConfig.baseUrl}. Configured: ${ApiConfig.isConfigured}");

    // Ensure ApiClient is using the loaded URL
    if (ApiConfig.isConfigured) {
      ApiClient().updateBaseUrl(ApiConfig.baseUrl);
    }

    // If configured and not web, verify connection
    if (!kIsWeb && ApiConfig.isConfigured) {
      AppLog.d("InitializeApp: Verifying connection to ${ApiConfig.health}");
      try {
        // Create a temporary Dio instance for health check with short timeout
        final dio = Dio(BaseOptions(
          baseUrl: ApiConfig.baseUrl,
          connectTimeout: const Duration(seconds: 5),
          receiveTimeout: const Duration(seconds: 5),
        ));

        // Check health to verify the server is reachable
        final response = await dio.get(ApiConfig.health);
        final healthData = response.data;
        AppLog.d("InitializeApp: Health check response: $healthData");

        if (!(healthData is Map && healthData['status'] == 'UP')) {
          throw Exception(
              "Health check failed: Invalid response format or status");
        }
        AppLog.d("InitializeApp: Health check passed");
      } catch (e) {
        AppLog.d(
            "InitializeApp: Backend unreachable at ${ApiConfig.baseUrl}: $e. Resetting config.");
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
      AppLog.d("InitializeApp: Fetching current user session");
      await SessionService.getCurrentUser();
    } else {
      AppLog.d("InitializeApp: Skipping session fetch (Not configured)");
    }

    AppLog.d("InitializeApp: Initialization complete");
  }
}
