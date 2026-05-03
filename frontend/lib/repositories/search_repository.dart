import 'package:dio/dio.dart';
import '../config/api_config.dart';
import '../exceptions/search_exception.dart';
import '../models/search_filters.dart';
import '../models/search_result.dart';
import '../services/api_client.dart';

/// Repository for transaction search operations.
///
/// Provides methods to search transactions with query string,
/// optional filters, pagination, and request cancellation support.
///
/// Validates: Requirements 7.1, 7.2, 7.5, 7.6
class SearchRepository {
  final ApiClient _api;

  SearchRepository({ApiClient? api}) : _api = api ?? ApiClient();

  /// Searches transactions matching the given query and filters.
  ///
  /// Parameters:
  /// - [query]: The search query string (1-200 characters)
  /// - [filters]: Optional filter criteria (date range, amount, accounts, category)
  /// - [page]: Zero-based page number for pagination (default: 0)
  /// - [size]: Number of results per page (default: 20)
  /// - [cancelToken]: Optional token to cancel the request
  ///
  /// Returns a [SearchResult] with matching transactions and metadata.
  ///
  /// Throws [SearchException] with categorized error types:
  /// - `network`: Connection issues or timeouts
  /// - `validation`: Invalid search parameters (400)
  /// - `server`: Server errors (500+)
  /// - `cancelled`: Request was cancelled by user
  Future<SearchResult> search({
    required String query,
    SearchFilters? filters,
    int page = 0,
    int size = 20,
    CancelToken? cancelToken,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'query': query,
        'page': page,
        'size': size,
        'expand': true,
        ...?filters?.toQueryParams(),
      };

      final response = await _api.get<Map<String, dynamic>>(
        ApiConfig.transactionSearch,
        query: queryParams,
        cancelToken: cancelToken,
      );

      return SearchResult.fromJson(response);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  /// Maps DioException to the appropriate SearchException type.
  SearchException _mapDioException(DioException e) {
    // Handle request cancellation
    if (e.type == DioExceptionType.cancel) {
      return SearchException.cancelled();
    }

    // Handle network connectivity issues
    if (e.type == DioExceptionType.connectionError ||
        e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout) {
      return SearchException.network(
        'Unable to connect. Please check your internet connection.',
      );
    }

    // Handle HTTP status codes
    final statusCode = e.response?.statusCode;

    if (statusCode == 400) {
      return SearchException.validation(
        'Invalid search parameters. Please check your filters.',
      );
    }

    // 401 and 403 are handled by ApiClient interceptor (auto-logout)
    // but we still map them here for completeness
    if (statusCode == 401 || statusCode == 403) {
      return SearchException.server(
        'Authentication failed. Please log in again.',
        statusCode ?? 401,
      );
    }

    // Handle all other server errors (5xx, etc.)
    return SearchException.server(
      'Something went wrong. Please try again.',
      statusCode ?? 500,
    );
  }
}
