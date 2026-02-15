import 'package:flutter/foundation.dart';
import 'package:logger/logger.dart';

class AppLog {
  static final Logger _logger = Logger(
    printer: SimplePrinter(
      colors: false,
      printTime: false,
    ),
  );

  static void d(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    if (!kDebugMode) return;
    _logger.d(message, error: error, stackTrace: stackTrace);
  }

  static void i(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    if (!kDebugMode) return;
    _logger.i(message, error: error, stackTrace: stackTrace);
  }

  static void w(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    if (!kDebugMode) return;
    _logger.w(message, error: error, stackTrace: stackTrace);
  }

  static void e(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    if (!kDebugMode) return;
    _logger.e(message, error: error, stackTrace: stackTrace);
  }
}
