import 'package:tracko/Utils/enums.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';
import 'package:tracko/services/api_client.dart';

class AnalyticsRepository {
  final ApiClient _api;

  AnalyticsRepository({ApiClient? api}) : _api = api ?? ApiClient();

  Future<AnalyticsChartResponse> getChartData({
    required String transactionType,
    required String startDate,
    required String endDate,
    required String granularity,
    String? groupBy,
    int? accountId,
    int? categoryId,
  }) async {
    final query = <String, dynamic>{
      'transactionType': transactionType,
      'startDate': startDate,
      'endDate': endDate,
      'granularity': granularity,
    };

    if (groupBy != null) query['groupBy'] = groupBy;
    if (accountId != null) query['accountId'] = accountId;
    if (categoryId != null) query['categoryId'] = categoryId;

    final res = await _api.get<Map<String, dynamic>>(
      '${ApiConfig.analytics}/chart',
      query: query,
    );

    return AnalyticsChartResponse.fromJson(res);
  }
}
