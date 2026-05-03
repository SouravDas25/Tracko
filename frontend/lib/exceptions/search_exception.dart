/// Error types that can occur during search operations.
enum SearchErrorType {
  /// Network connectivity issues
  network,
  
  /// Server-side errors (5xx responses)
  server,
  
  /// Input validation failures
  validation,
  
  /// User cancelled the search
  cancelled,
}

/// Exception thrown during search operations.
/// 
/// Provides structured error information with categorized error types
/// and optional HTTP status codes for server errors.
/// 
/// Validates: Requirements 6.1, 6.2, 6.3, 7.5
class SearchException implements Exception {
  /// Human-readable error message
  final String message;
  
  /// Categorized error type
  final SearchErrorType type;
  
  /// HTTP status code (only set for server errors)
  final int? statusCode;
  
  const SearchException({
    required this.message,
    required this.type,
    this.statusCode,
  });
  
  /// Creates a network connectivity error exception.
  factory SearchException.network(String message) {
    return SearchException(
      message: message,
      type: SearchErrorType.network,
    );
  }
  
  /// Creates a server error exception with HTTP status code.
  factory SearchException.server(String message, int statusCode) {
    return SearchException(
      message: message,
      type: SearchErrorType.server,
      statusCode: statusCode,
    );
  }
  
  /// Creates a validation error exception.
  factory SearchException.validation(String message) {
    return SearchException(
      message: message,
      type: SearchErrorType.validation,
    );
  }
  
  /// Creates a cancelled search exception.
  factory SearchException.cancelled() {
    return const SearchException(
      message: 'Search was cancelled',
      type: SearchErrorType.cancelled,
    );
  }
  
  @override
  String toString() {
    if (statusCode != null) {
      return 'SearchException: $message (type: $type, statusCode: $statusCode)';
    }
    return 'SearchException: $message (type: $type)';
  }
}
