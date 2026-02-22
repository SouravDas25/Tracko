import 'package:dio/dio.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/Utils/AppLog.dart';

class HealthCheckUtil {
  /// Checks if the backend at [baseUrl] is reachable and healthy.
  /// Returns true if the health check passes, false otherwise.
  static Future<bool> checkHealth(String baseUrl) async {
    try {
      // Ensure clean URL
      var cleanUrl = baseUrl;
      if (cleanUrl.endsWith('/')) {
        cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1);
      }

      final dio = Dio(BaseOptions(
        baseUrl: cleanUrl,
        connectTimeout: const Duration(seconds: 5),
        receiveTimeout: const Duration(seconds: 5),
      ));

      AppLog.d("HealthCheckUtil: Checking $cleanUrl${ApiConfig.health}");
      final response = await dio.get(ApiConfig.health);
      final healthData = response.data;

      if (healthData is Map && healthData['status'] == 'UP') {
        AppLog.d("HealthCheckUtil: Health check passed");
        return true;
      }
      AppLog.d("HealthCheckUtil: Health check failed with response: $healthData");
      return false;
    } catch (e) {
      AppLog.d("HealthCheckUtil: Health check failed: $e");
      return false;
    }
  }
}
